package de.lind3.CloudLite.folder;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a folder.
 *
 * @param id        folder identifier
 * @param name      folder name
 * @param path      full folder path
 * @param parentId  ID of the parent folder, or {@code null} for root folders
 * @param ownerId   ID of the folder owner
 * @param createdAt creation timestamp
 * @param updatedAt last update timestamp
 */
public record FolderResponse(
        UUID id,
        String name,
        String path,
        UUID parentId,
        UUID ownerId,
        Instant createdAt,
        Instant updatedAt
) {
    public static FolderResponse from(FolderEntity entity) {
        return new FolderResponse(
                entity.getId(),
                entity.getName(),
                entity.getPath(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.getOwner().getId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
