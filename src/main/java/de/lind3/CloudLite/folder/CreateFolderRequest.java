package de.lind3.CloudLite.folder;

import java.util.UUID;

/**
 * Request body for creating a new folder.
 *
 * @param name     folder name (must be unique within the parent for this owner)
 * @param parentId ID of the parent folder, or {@code null} to create a root folder
 */
public record CreateFolderRequest(String name, UUID parentId) {}
