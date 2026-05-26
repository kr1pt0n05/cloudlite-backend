package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.file.FileEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a file written to its final filesystem path.
 *
 * @param id        file identifier
 * @param name      filename inside the folder
 * @param folderId  parent folder
 * @param storagePath relative filesystem path below the configured storage root
 * @param sizeBytes byte count
 * @param sha256    hex SHA-256 digest
 * @param mimeType  MIME type, or {@code null} if unknown
 * @param createdAt creation timestamp
 */
public record PublishedFileResponse(
        UUID id,
        String name,
        UUID folderId,
        String storagePath,
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
                entity.getStoragePath(),
                entity.getSizeBytes(),
                entity.getSha256(),
                entity.getMimeType(),
                entity.getCreatedAt()
        );
    }
}
