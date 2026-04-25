package de.lind3.CloudLite.service;

import de.lind3.CloudLite.entity.FileEntity;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Operations on committed files: retrieval, download, move/rename, and soft-delete.
 * All mutating operations verify that the requester is the file owner.
 */
public interface FileService {

    /**
     * Returns file metadata.
     *
     * @param fileId           ID of the file
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return the file entity
     */
    FileEntity getFile(UUID fileId, String requesterSubject);

    /**
     * Lists all non-deleted files inside a folder.
     *
     * @param folderId         the folder to list
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return list of file entities
     */
    List<FileEntity> listFiles(UUID folderId, String requesterSubject);

    /**
     * Opens a stream to download the file's current blob content.
     * The caller is responsible for closing the returned stream.
     *
     * @param fileId           ID of the file
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return raw byte stream of the current file version
     * @throws IOException if the blob cannot be read from storage
     */
    InputStream downloadFile(UUID fileId, String requesterSubject) throws IOException;

    /**
     * Renames a file within its current folder.
     * Fails if the new name is already taken in the folder.
     *
     * @param fileId           ID of the file
     * @param newName          new filename
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return updated file entity
     */
    FileEntity renameFile(UUID fileId, String newName, String requesterSubject);

    /**
     * Moves a file to a different folder.
     * Fails if a file with the same name already exists in the target folder.
     *
     * @param fileId           ID of the file
     * @param targetFolderId   destination folder
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return updated file entity
     */
    FileEntity moveFile(UUID fileId, UUID targetFolderId, String requesterSubject);

    /**
     * Soft-deletes a file by setting its {@code deletedAt} timestamp.
     *
     * @param fileId           ID of the file
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     */
    void deleteFile(UUID fileId, String requesterSubject);
}
