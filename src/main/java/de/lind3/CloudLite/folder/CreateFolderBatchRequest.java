package de.lind3.CloudLite.folder;

import java.util.List;
import java.util.UUID;

/**
 * Request body for creating multiple folders as a tree.
 *
 * @param parentId    ID of the existing parent folder, or {@code null} to create root folders
 * @param directories root directory nodes to create under the parent
 */
public record CreateFolderBatchRequest(
        UUID parentId,
        List<CreateFolderTreeNodeRequest> directories
) {
}
