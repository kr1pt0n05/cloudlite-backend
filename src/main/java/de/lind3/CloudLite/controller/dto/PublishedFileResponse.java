package de.lind3.CloudLite.controller.dto;

import de.lind3.CloudLite.entity.FileEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a published file (after session commit).
 *
 * @param id        file identifier
 * @param name      filename inside the folder
 * @param folderId  parent folder
 * @param blobId    underlying blob identifier
 * @param sizeBytes byte count
 * @param sha256    hex SHA-256 digest
 * @param mimeType  MIME type, or {@code null} if unknown
 * @param createdAt creation timestamp
 */
public record PublishedFileResponse(
        UUID id,
        String name,
        UUID folderId,
        UUID blobId,
        long sizeBytes,
        String sha256,
        String mimeType,
        Instant createdAt
) {
    public static PublishedFileResponse from(FileEntity entity) {
        return new PublishedFileResponse(
                entity.getId(),
                entity.getName(),
                entity.getFolder().getId(),
                entity.getBlob().getId(),
                entity.getBlob().getSizeBytes(),
                entity.getBlob().getSha256(),
                entity.getBlob().getMimeType(),
                entity.getCreatedAt()
        );
    }
}
