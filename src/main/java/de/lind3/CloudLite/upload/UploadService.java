package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.file.FileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of an upload session (batch-upload flow):
 * <ol>
 *   <li>Create a session for a batch upload.</li>
 *   <li>Upload one or more files with a client-file-id to folder-path mapping.</li>
 *   <li>Validate database conflicts before streaming files to their final filesystem paths.</li>
 *   <li>Insert committed file rows after all filesystem writes succeed.</li>
 * </ol>
 */
public interface UploadService {

    /**
     * Opens a new upload session for the authenticated user.
     *
     * @param ownerSubject the JWT {@code sub} claim of the requesting user
     * @return the newly created session
     */
    UploadSessionEntity createSession(String ownerSubject);

    /**
     * Validates and streams multiple files into their mapped destination folders.
     * <p>
     * SHA-256 and byte count are computed during the single streaming write for each file —
     * content is never fully buffered in heap memory.
     * Database conflicts are checked before any filesystem write occurs.
     *
     * @param sessionId    the session to add files to
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     * @param files        one or more multipart files; each filename must be prefixed
     *                     with its client-generated file ID
     * @param mapping      client file ID to destination folder path mapping
     * @return the list of committed {@link FileEntity} records
     * @throws IOException if any filesystem write fails
     */
    List<FileEntity> uploadFilesBatch(UUID sessionId, String ownerSubject, List<MultipartFile> files,
                                      BatchUploadFileMappingRequest mapping) throws IOException;

    /**
     * Cancels an OPEN session.
     *
     * @param sessionId    the session to cancel
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     */
    void cancelSession(UUID sessionId, String ownerSubject);
}
