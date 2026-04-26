package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.blob.BlobEntity;
import de.lind3.CloudLite.blob.BlobRepository;
import de.lind3.CloudLite.blob.BlobStorageService;
import de.lind3.CloudLite.blob.BlobWriteResult;
import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.file.FileRepository;
import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.folder.FolderService;
import de.lind3.CloudLite.user.UserEntity;
import de.lind3.CloudLite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UserRepository userRepository;
    private final FolderService folderService;
    private final FileRepository fileRepository;
    private final BlobRepository blobRepository;
    private final UploadSessionRepository sessionRepository;
    private final UploadSessionFileRepository sessionFileRepository;
    private final BlobStorageService blobStorageService;

    // -------------------------------------------------------------------------
    // createSession
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public UploadSessionEntity createSession(String ownerSubject, UUID targetFolderId) {
        UserEntity owner = resolveOrProvisionUser(ownerSubject);

        FolderEntity folder = folderService.getFolder(targetFolderId, ownerSubject);

        UploadSessionEntity session = new UploadSessionEntity();
        session.setOwner(owner);
        session.setTargetFolder(folder);
        return sessionRepository.save(session);
    }

    // -------------------------------------------------------------------------
    // stageFilesBatch
    // -------------------------------------------------------------------------

    /**
     * Streams all files to blob storage and stages their metadata in the session.
     *
     * <p>Duplicate-name validation is performed with a single IN-query before any I/O.
     * All blob writes happen outside a transaction; a single transactional batch insert
     * records all staged-file metadata at the end.
     *
     * <p>If storage or persistence fails, every blob written so far is deleted on a
     * best-effort basis to avoid orphaned files.
     */
    @Override
    public List<UploadSessionFileEntity> stageFilesBatch(UUID sessionId, String ownerSubject,
                                                         List<MultipartFile> files) throws IOException {
        if (files.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one file is required");
        }

        // --- pre-flight checks (short reads, no long-lived TX) ---
        UserEntity owner = userRepository.findBySubject(ownerSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UploadSessionEntity session = sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != UploadSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Session is not OPEN (current status: " + session.getStatus() + ")");
        }

        // Collect and validate filenames
        List<String> names = new ArrayList<>(files.size());
        for (MultipartFile file : files) {
            String name = file.getOriginalFilename();
            if (name == null || name.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
            }
            if (names.contains(name)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Duplicate filename in request: " + name);
            }
            names.add(name);
        }

        // Single query to check for already-staged names
        List<String> existing = sessionFileRepository.findExistingFileNamesInSession(session, names);
        if (!existing.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Files already staged in this session: " + String.join(", ", existing));
        }

        // --- stream all files to storage (outside any transaction) ---
        // results contains only successfully completed writes; any store() that throws
        // leaves nothing to clean up from that call (the blob was never fully persisted).
        List<BlobWriteResult> results = new ArrayList<>(files.size());
        try {
            for (MultipartFile file : files) {
                results.add(blobStorageService.store(file.getInputStream(), UUID.randomUUID().toString()));
            }
        } catch (IOException | RuntimeException e) {
            deleteBlobs(results);
            throw e;
        }

        // --- persist all metadata in a single transaction ---
        try {
            return persistStagedFilesBatch(session, files, results);
        } catch (RuntimeException e) {
            deleteBlobs(results);
            throw e;
        }
    }

    @Transactional
    protected List<UploadSessionFileEntity> persistStagedFilesBatch(UploadSessionEntity session,
                                                                     List<MultipartFile> files,
                                                                     List<BlobWriteResult> results) {
        List<UploadSessionFileEntity> entities = new ArrayList<>(files.size());
        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            BlobWriteResult result = results.get(i);

            UploadSessionFileEntity entity = new UploadSessionFileEntity();
            entity.setSession(session);
            entity.setFileName(file.getOriginalFilename());
            entity.setStorageKey(result.storageKey());
            entity.setSha256(result.sha256());
            entity.setSizeBytes(result.sizeBytes());
            entity.setMimeType(file.getContentType());
            entities.add(entity);
        }
        return sessionFileRepository.saveAll(entities);
    }

    // -------------------------------------------------------------------------
    // commitSession
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public List<FileEntity> commitSession(UUID sessionId, String ownerSubject) {
        UserEntity owner = resolveOrProvisionUser(ownerSubject);

        UploadSessionEntity session = sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != UploadSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Session is not OPEN (current status: " + session.getStatus() + ")");
        }

        List<UploadSessionFileEntity> staged = sessionFileRepository
                .findBySessionAndStatus(session, UploadSessionFileStatus.UPLOADED);

        FolderEntity targetFolder = session.getTargetFolder();

        // Validate all names before creating any FileEntity (fail-fast, atomic)
        for (UploadSessionFileEntity sf : staged) {
            if (fileRepository.existsByFolderAndNameAndDeletedAtIsNull(targetFolder, sf.getFileName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A file named '" + sf.getFileName() + "' already exists in the target folder");
            }
        }

        // Create BlobEntity records from inline staged metadata (batch)
        List<BlobEntity> blobs = new ArrayList<>(staged.size());
        for (UploadSessionFileEntity sf : staged) {
            BlobEntity blob = new BlobEntity();
            blob.setStorageKey(sf.getStorageKey());
            blob.setSha256(sf.getSha256());
            blob.setSizeBytes(sf.getSizeBytes());
            blob.setMimeType(sf.getMimeType());
            blobs.add(blob);
        }
        blobRepository.saveAll(blobs);

        // Create FileEntity records linked to the new blobs
        List<FileEntity> published = new ArrayList<>(staged.size());
        for (int i = 0; i < staged.size(); i++) {
            UploadSessionFileEntity sf = staged.get(i);
            FileEntity file = new FileEntity();
            file.setFolder(targetFolder);
            file.setName(sf.getFileName());
            file.setOwner(owner);
            file.setBlob(blobs.get(i));
            sf.setStatus(UploadSessionFileStatus.COMMITTED);
            published.add(file);
        }

        fileRepository.saveAll(published);
        sessionFileRepository.saveAll(staged);

        session.setStatus(UploadSessionStatus.COMMITTED);
        sessionRepository.save(session);

        return published;
    }

    // -------------------------------------------------------------------------
    // cancelSession
    // -------------------------------------------------------------------------

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
        // Orphaned blobs are cleaned up by a maintenance task
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    /**
     * Returns the {@link UserEntity} for the given JWT subject, creating one if
     * this is the user's first interaction with the system (JIT provisioning).
     */
    private UserEntity resolveOrProvisionUser(String subject) {
        return userRepository.findBySubject(subject)
                .orElseGet(() -> userRepository.save(new UserEntity(subject)));
    }

    /** Best-effort deletion of blobs that were written before a failure. */
    private void deleteBlobs(List<BlobWriteResult> results) {
        for (BlobWriteResult r : results) {
            try {
                blobStorageService.delete(r.storageKey());
            } catch (IOException ignored) {
                // Log in production; acceptable for MVP
            }
        }
    }
}
