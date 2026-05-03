package de.lind3.CloudLite.folder;

import de.lind3.CloudLite.folder.FolderEntity;

import java.util.List;
import java.util.UUID;

/**
 * Operations on the virtual folder hierarchy: create, rename, move, list, and soft-delete.
 * All mutating operations verify that the requester is the folder owner.
 */
public interface FolderService {

    /**
     * Creates a new folder.
     *
     * @param name             folder name (must be unique within the parent for this owner)
     * @param parentId         ID of the parent folder, or {@code null} to create a root folder
     * @param ownerSubject     JWT {@code sub} claim of the requesting user
     * @return the newly created folder
     */
    FolderEntity createFolder(String name, UUID parentId, String ownerSubject);

    /**
     * Creates multiple folders as a tree.
     * New parent-child relationships and paths are assigned before persisting the
     * folders in one batch.
     *
     * @param parentId         ID of the existing parent folder, or {@code null} to create root folders
     * @param directories      root directory nodes to create
     * @param ownerSubject     JWT {@code sub} claim of the requesting user
     * @return newly created folders in pre-order traversal
     */
    List<FolderEntity> createFoldersBatch(
            UUID parentId,
            List<CreateFolderTreeNodeRequest> directories,
            String ownerSubject
    );

    /**
     * Returns all non-deleted direct children of the given folder.
     *
     * @param folderId         ID of the parent folder
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return list of child folders
     */
    List<FolderEntity> listSubFolders(UUID folderId, String requesterSubject);

    /**
     * Renames a folder within its current parent.
     * Fails if the new name is already taken in the parent.
     *
     * @param folderId         ID of the folder to rename
     * @param newName          new folder name
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return updated folder entity
     */
    FolderEntity renameFolder(UUID folderId, String newName, String requesterSubject);

    /**
     * Moves a folder to a new parent.
     * Fails if a folder with the same name already exists in the target parent,
     * or if moving would create a cycle.
     *
     * @param folderId         ID of the folder to move
     * @param newParentId      destination parent folder ID, or {@code null} to move to root
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return updated folder entity
     */
    FolderEntity moveFolder(UUID folderId, UUID newParentId, String requesterSubject);

    /**
     * Soft-deletes a folder and (recursively) all its contents.
     *
     * @param folderId         ID of the folder to delete
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     */
    void deleteFolder(UUID folderId, String requesterSubject);

    /**
     * Returns the folder with the given ID, verifying it belongs to the requester.
     *
     * @param folderId         ID of the folder to retrieve
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return the folder entity
     */
    FolderEntity getFolder(UUID folderId, String requesterSubject);

    /**
     * Returns {@code true} if a non-deleted folder with the given ID exists.
     *
     * @param folderId ID of the folder to check
     * @return whether the folder exists
     */
    boolean folderExists(UUID folderId);
}
