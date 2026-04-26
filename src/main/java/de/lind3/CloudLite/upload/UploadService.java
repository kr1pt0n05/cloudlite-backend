package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.upload.UploadSessionEntity;
import de.lind3.CloudLite.upload.UploadSessionFileEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of an upload session (batch-upload flow):
 * <ol>
 *   <li>Create a session targeting a destination folder.</li>
 *   <li>Stage one or more files into the session; each call streams binary content to
 *       storage and records metadata without making files visible to other users.</li>
 *   <li>Commit the session atomically, publishing all staged files into the folder.</li>
 *   <li>Optionally cancel an open session, cleaning up staged blobs.</li>
 * </ol>
 */
public interface UploadService {

    /**
     * Opens a new upload session for the authenticated user.
     *
     * @param ownerSubject   the JWT {@code sub} claim of the requesting user
     * @param targetFolderId destination folder where files will be published on commit
     * @return the newly created session
     */
    UploadSessionEntity createSession(String ownerSubject, UUID targetFolderId);

    /**
     * Streams multiple files into the session in a single call.
     * Each file's binary content is written to the storage backend and staged for commit;
     * files are not yet visible to other users.
     * <p>
     * SHA-256 and byte count are computed during the single streaming write for each file —
     * content is never fully buffered in heap memory.
     * All duplicate-name checks are performed with a single database query before any writes.
     *
     * @param sessionId    the session to add files to
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     * @param files        one or more multipart files; each file's {@code Content-Disposition}
     *                     filename is used as the staged filename
     * @return the list of staged {@link UploadSessionFileEntity} records with status UPLOADED
     * @throws IOException if any blob write fails
     */
    List<UploadSessionFileEntity> stageFilesBatch(UUID sessionId, String ownerSubject,
                                                  List<MultipartFile> files) throws IOException;

    /**
     * Atomically publishes all UPLOADED files in the session to the target folder.
     * {@link de.lind3.CloudLite.blob.BlobEntity} records are created from the staged inline
     * metadata and linked to the new {@link FileEntity} records in a single transaction.
     * The session status is set to COMMITTED and cannot be modified afterwards.
     *
     * @param sessionId    the session to commit
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     * @return the list of newly visible {@link FileEntity} records
     */
    List<FileEntity> commitSession(UUID sessionId, String ownerSubject);

    /**
     * Cancels an OPEN session, removing staged blobs and setting status to CANCELLED.
     *
     * @param sessionId    the session to cancel
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     */
    void cancelSession(UUID sessionId, String ownerSubject);
}
