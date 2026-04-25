package de.lind3.CloudLite.service;

import de.lind3.CloudLite.entity.FileEntity;
import de.lind3.CloudLite.entity.UploadSessionEntity;
import de.lind3.CloudLite.entity.UploadSessionFileEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Manages the lifecycle of an upload session (batch-upload flow):
 * <ol>
 *   <li>Create a session targeting a destination folder.</li>
 *   <li>Stream individual files into the session; each call persists the blob
 *       and stages metadata without making files visible.</li>
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
     * Streams a single file into the session.
     * The blob is written to storage and staged for commit; the file is not yet visible
     * to other users.
     *
     * @param sessionId    the session to add the file to
     * @param ownerSubject the JWT {@code sub} claim used to verify session ownership
     * @param fileName     the filename as it should appear after commit
     * @param content      raw file bytes as a streaming {@link InputStream}
     * @return the staged {@link UploadSessionFileEntity} with status UPLOADED
     * @throws IOException if the blob write fails
     */
    UploadSessionFileEntity stageFile(UUID sessionId, String ownerSubject, String fileName, InputStream content)
            throws IOException;

    /**
     * Atomically publishes all UPLOADED files in the session to the target folder.
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
