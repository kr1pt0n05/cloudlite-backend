package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.changelog.ChangeLogEntity;
import de.lind3.CloudLite.changelog.ChangeLogService;
import de.lind3.CloudLite.changelog.EntityType;
import de.lind3.CloudLite.changelog.EventType;
import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.file.FileRepository;
import de.lind3.CloudLite.file.FileStorageService;
import de.lind3.CloudLite.file.FileWriteResult;
import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.folder.FolderRepository;
import de.lind3.CloudLite.user.UserEntity;
import de.lind3.CloudLite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadServiceImpl implements UploadService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final UploadSessionRepository sessionRepository;
    private final FileStorageService fileStorageService;
    private final ChangeLogService changeLogService;

    @Override
    @Transactional
    public UploadSessionEntity createSession(String ownerSubject) {
        UserEntity owner = resolveOrProvisionUser(ownerSubject);

        UploadSessionEntity session = new UploadSessionEntity();
        session.setOwner(owner);
        return sessionRepository.save(session);
    }

    @Override
    @Transactional
    public List<FileEntity> uploadFilesBatch(UUID sessionId, String ownerSubject, List<MultipartFile> files,
                                             BatchUploadFileMappingRequest mapping) throws IOException {
        UserEntity owner = resolveOrProvisionUser(ownerSubject);
        UploadSessionEntity session = resolveOpenSession(sessionId, owner);
        List<PlannedUpload> plannedUploads = planAndValidateUploads(owner, files, mapping);

        List<FileWriteResult> writeResults = new ArrayList<>(plannedUploads.size());
        try {
            for (PlannedUpload plannedUpload : plannedUploads) {
                writeResults.add(fileStorageService.store(
                        ownerSubject,
                        plannedUpload.folder().getPath(),
                        plannedUpload.fileName(),
                        plannedUpload.file().getInputStream()));
            }
            return persistUploadedFiles(session, owner, plannedUploads, writeResults);
        } catch (IOException | RuntimeException e) {
            fileStorageService.deleteAll(writeResults);
            throw e;
        }
    }

    @Override
    @Transactional
    public void cancelSession(UUID sessionId, String ownerSubject) {
        UserEntity owner = resolveOrProvisionUser(ownerSubject);
        UploadSessionEntity session = sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != UploadSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Session is not OPEN (current status: " + session.getStatus() + ")");
        }

        session.setStatus(UploadSessionStatus.CANCELLED);
        sessionRepository.save(session);
    }

    @Transactional
    protected List<FileEntity> persistUploadedFiles(UploadSessionEntity session, UserEntity owner,
                                                    List<PlannedUpload> plannedUploads,
                                                    List<FileWriteResult> writeResults) {
        List<FileEntity> fileEntities = new ArrayList<>(plannedUploads.size());
        for (int i = 0; i < plannedUploads.size(); i++) {
            PlannedUpload plannedUpload = plannedUploads.get(i);
            FileWriteResult writeResult = writeResults.get(i);

            FileEntity file = new FileEntity();
            file.setName(plannedUpload.fileName());
            file.setFolder(plannedUpload.folder());
            file.setOwner(owner);
            file.setStoragePath(writeResult.storagePath());
            file.setSha256(writeResult.sha256());
            file.setSizeBytes(writeResult.sizeBytes());
            file.setMimeType(plannedUpload.file().getContentType());
            fileEntities.add(file);
        }

        List<FileEntity> savedFiles = fileRepository.saveAll(fileEntities);
        fileRepository.flush();
        session.setStatus(UploadSessionStatus.COMMITTED);
        sessionRepository.save(session);
        changeLogService.logChanges(buildCommittedFileChangeLogs(savedFiles, owner));
        return savedFiles;
    }

    private UploadSessionEntity resolveOpenSession(UUID sessionId, UserEntity owner) {
        UploadSessionEntity session = sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != UploadSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Session is not OPEN (current status: " + session.getStatus() + ")");
        }
        return session;
    }

    private List<PlannedUpload> planAndValidateUploads(UserEntity owner, List<MultipartFile> files,
                                                       BatchUploadFileMappingRequest mapping) {
        if (files == null || files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }
        if (mapping == null || mapping.files() == null || mapping.files().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A fileId-to-path mapping is required for every file");
        }
        if (mapping.files().size() != files.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Expected " + files.size() + " file mappings, received " + mapping.files().size());
        }

        Map<String, String> folderPathsByFileId = validateMapping(mapping);
        List<PlannedUpload> plannedUploads = new ArrayList<>(files.size());
        Set<String> targetNamesInRequest = new HashSet<>();

        for (MultipartFile file : files) {
            ClientFileName clientFileName = parseClientFileName(file);
            String folderPath = folderPathsByFileId.remove(clientFileName.fileId());
            if (folderPath == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No folder path mapping found for fileId: " + clientFileName.fileId());
            }

            FolderEntity folder = folderRepository.findByOwnerAndPathAndDeletedAtIsNull(owner, folderPath)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Folder not found for path: " + folderPath));

            String targetKey = folder.getId() + "\n" + clientFileName.fileName();
            if (!targetNamesInRequest.add(targetKey)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate filename in upload target folder: " + clientFileName.fileName());
            }
            if (fileRepository.existsByFolderAndNameAndDeletedAtIsNull(folder, clientFileName.fileName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A file named '" + clientFileName.fileName() + "' already exists in " + folderPath);
            }
            if (fileStorageService.exists(owner.getSubject(), folder.getPath(), clientFileName.fileName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A file already exists on disk at " + folderPath + "/" + clientFileName.fileName());
            }

            plannedUploads.add(new PlannedUpload(file, clientFileName.fileName(), folder));
        }

        if (!folderPathsByFileId.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mappings were provided for files not present in this request: "
                            + String.join(", ", folderPathsByFileId.keySet()));
        }
        return plannedUploads;
    }

    private Map<String, String> validateMapping(BatchUploadFileMappingRequest mapping) {
        Map<String, String> folderPathsByFileId = new HashMap<>(mapping.files().size());
        for (UploadFileMappingRequest fileMapping : mapping.files()) {
            if (fileMapping == null || fileMapping.fileId() == null || fileMapping.fileId().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each mapping must include fileId");
            }
            if (fileMapping.path() == null || fileMapping.path().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each mapping must include path");
            }
            String fileId = fileMapping.fileId().trim();
            try {
                UUID.fromString(fileId);
            } catch (IllegalArgumentException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "fileId must be a UUID: " + fileId);
            }
            String folderPath = normalizeFolderPath(fileMapping.path());
            String previous = folderPathsByFileId.put(fileId, folderPath);
            if (previous != null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate mapping for fileId: " + fileId);
            }
        }
        return folderPathsByFileId;
    }

    private ClientFileName parseClientFileName(MultipartFile file) {
        if (file == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "file part must not be null");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
        }

        String clientName = originalFilename.replace("\\", "/");
        int lastSlash = clientName.lastIndexOf('/');
        if (lastSlash >= 0) {
            clientName = clientName.substring(lastSlash + 1);
        }

        UUID fileId = parseUuidPrefix(clientName);
        String fileName = clientName.substring(fileId.toString().length());
        if (fileName.startsWith("-") || fileName.startsWith("_")) {
            fileName = fileName.substring(1);
        }
        fileName = validateFileName(fileName, originalFilename);
        return new ClientFileName(fileId.toString(), fileName);
    }

    private UUID parseUuidPrefix(String fileName) {
        if (fileName.length() < 36) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Multipart filename must be prefixed with a UUID: " + fileName);
        }
        try {
            return UUID.fromString(fileName.substring(0, 36));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Multipart filename must be prefixed with a UUID: " + fileName);
        }
    }

    private String validateFileName(String fileName, String originalFilename) {
        String trimmedName = fileName == null ? "" : fileName.trim();
        if (trimmedName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Filename after UUID prefix must not be blank: " + originalFilename);
        }
        if (".".equals(trimmedName) || "..".equals(trimmedName)
                || trimmedName.contains("/") || trimmedName.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Filename must be a single path segment: " + originalFilename);
        }
        return trimmedName;
    }

    private String normalizeFolderPath(String path) {
        String normalizedPath = path.trim().replace("\\", "/");
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        while (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        for (String segment : normalizedPath.substring(1).split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Path must not contain empty, '.', or '..' segments: " + path);
            }
        }
        return normalizedPath;
    }

    private UserEntity resolveOrProvisionUser(String subject) {
        return userRepository.findBySubject(subject)
                .orElseGet(() -> userRepository.save(new UserEntity(subject)));
    }

    private List<ChangeLogEntity> buildCommittedFileChangeLogs(List<FileEntity> files, UserEntity user) {
        List<ChangeLogEntity> changeLogs = new ArrayList<>(files.size());
        for (FileEntity file : files) {
            ChangeLogEntity changeLog = new ChangeLogEntity();
            changeLog.setEventType(EventType.CREATE);
            changeLog.setEntityType(EntityType.FILE);
            changeLog.setFile(file);
            changeLog.setUser(user);
            changeLogs.add(changeLog);
        }
        return changeLogs;
    }

    private record PlannedUpload(MultipartFile file, String fileName, FolderEntity folder) {}

    private record ClientFileName(String fileId, String fileName) {}
}
