package de.lind3.CloudLite.folder;

import java.util.List;

/**
 * Folder node in a batch directory creation tree.
 *
 * @param name     folder name
 * @param children child folders to create below this folder
 */
public record CreateFolderTreeNodeRequest(
        String name,
        List<CreateFolderTreeNodeRequest> children
) {
}
