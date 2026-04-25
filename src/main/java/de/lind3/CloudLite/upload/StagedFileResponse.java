package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.upload.UploadSessionFileEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a file staged inside an upload session.
 *
 * @param id        staged-file record identifier
 * @param fileName  name the file will have after commit
 * @param blobId    identifier of the underlying blob (useful for dedup diagnostics)
 * @param sizeBytes byte count of the stored content
 * @param sha256    hex SHA-256 digest of the content
 * @param status    current staged-file status
 * @param createdAt timestamp when the file was staged
 */
public record StagedFileResponse(
        UUID id,
        String fileName,
        UUID blobId,
        long sizeBytes,
        String sha256,
        String status,
        Instant createdAt
) {
    public static StagedFileResponse from(UploadSessionFileEntity entity) {
        return new StagedFileResponse(
                entity.getId(),
                entity.getFileName(),
                entity.getBlob().getId(),
                entity.getBlob().getSizeBytes(),
                entity.getBlob().getSha256(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
