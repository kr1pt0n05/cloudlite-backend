package de.lind3.CloudLite.service.impl;

import de.lind3.CloudLite.entity.*;
import de.lind3.CloudLite.entity.enums.UploadSessionFileStatus;
import de.lind3.CloudLite.entity.enums.UploadSessionStatus;
import de.lind3.CloudLite.repository.*;
import de.lind3.CloudLite.service.UploadService;
import de.lind3.CloudLite.storage.BlobStorageService;
import de.lind3.CloudLite.storage.BlobWriteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
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

        FolderEntity folder = folderRepository.findByIdAndDeletedAtIsNull(targetFolderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Folder not found: " + targetFolderId));

        if (!folder.getOwner().getId().equals(owner.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Folder does not belong to the requesting user");
        }

        UploadSessionEntity session = new UploadSessionEntity();
        session.setOwner(owner);
        session.setTargetFolder(folder);
        return sessionRepository.save(session);
    }

    // -------------------------------------------------------------------------
    // stageFile
    // -------------------------------------------------------------------------

    /**
     * Streams the file to blob storage and stages metadata in the session.
     *
     * <p>The SHA-256 digest and byte count are derived from a single streaming write via
     * {@link BlobStorageService#store} — the content is never loaded into heap memory.
     * If an identical blob already exists (matched by SHA-256) the newly written bytes are
     * removed from storage and the existing {@link BlobEntity} is reused (deduplication).
     *
     * <p>The database transaction is opened <em>after</em> the blob has been written to
     * avoid holding a DB connection open during potentially long I/O. If the transaction
     * fails after the write, the orphaned blob will be cleaned up by a future maintenance
     * task.
     */
    @Override
    public UploadSessionFileEntity stageFile(UUID sessionId, String ownerSubject,
                                             String fileName, InputStream content,
                                             String mimeType) throws IOException {
        // --- pre-flight checks (short reads, no long-lived TX) ---
        UserEntity owner = userRepository.findBySubject(ownerSubject)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        UploadSessionEntity session = sessionRepository.findByIdAndOwner(sessionId, owner)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Session not found"));

        if (session.getStatus() != UploadSessionStatus.OPEN) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Session is not OPEN (current status: " + session.getStatus() + ")");
        }
        if (fileName == null || fileName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fileName must not be blank");
        }
        if (sessionFileRepository.existsBySessionAndFileName(session, fileName)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A file named '" + fileName + "' is already staged in this session");
        }

        // --- stream content to storage (outside any transaction) ---
        String suggestedKey = UUID.randomUUID().toString();
        BlobWriteResult result = blobStorageService.store(content, suggestedKey);

        // --- persist metadata in a transaction ---
        try {
            return persistStagedFile(session, fileName, mimeType, result);
        } catch (RuntimeException e) {
            // Best-effort cleanup of the orphaned blob if the DB write fails
            try {
                blobStorageService.delete(result.storageKey());
            } catch (IOException ignored) {
                // Log in production; acceptable for MVP
            }
            throw e;
        }
    }

    @Transactional
    protected UploadSessionFileEntity persistStagedFile(UploadSessionEntity session,
                                                        String fileName,
                                                        String mimeType,
                                                        BlobWriteResult result) throws IOException {
        // Deduplication: reuse existing blob if SHA-256 matches
        BlobEntity blob = blobRepository.findBySha256(result.sha256())
                .orElseGet(() -> {
                    BlobEntity newBlob = new BlobEntity();
                    newBlob.setStorageKey(result.storageKey());
                    newBlob.setSha256(result.sha256());
                    newBlob.setSizeBytes(result.sizeBytes());
                    newBlob.setMimeType(mimeType);
                    return blobRepository.save(newBlob);
                });

        // If a duplicate blob was found, the newly written bytes are orphaned on disk.
        // A maintenance task is responsible for cleaning those up.
        if (!blob.getStorageKey().equals(result.storageKey())) {
            try {
                blobStorageService.delete(result.storageKey());
            } catch (IOException e) {
                throw new RuntimeException("Failed to delete duplicate blob from storage", e);
            }
        }

        UploadSessionFileEntity sessionFile = new UploadSessionFileEntity();
        sessionFile.setSession(session);
        sessionFile.setFileName(fileName);
        sessionFile.setBlob(blob);
        sessionFile.setStatus(UploadSessionFileStatus.UPLOADED);
        return sessionFileRepository.save(sessionFile);
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

        List<FileEntity> published = staged.stream().map(sf -> {
            FileEntity file = new FileEntity();
            file.setFolder(targetFolder);
            file.setName(sf.getFileName());
            file.setOwner(owner);
            file.setBlob(sf.getBlob());
            sf.setStatus(UploadSessionFileStatus.COMMITTED);
            return file;
        }).toList();

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
}
