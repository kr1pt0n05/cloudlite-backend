package de.lind3.CloudLite.controller.dto;

import de.lind3.CloudLite.entity.UploadSessionEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an upload session.
 *
 * @param id             session identifier
 * @param targetFolderId destination folder
 * @param status         current session status
 * @param createdAt      creation timestamp
 */
public record SessionResponse(
        UUID id,
        UUID targetFolderId,
        String status,
        Instant createdAt
) {
    public static SessionResponse from(UploadSessionEntity entity) {
        return new SessionResponse(
                entity.getId(),
                entity.getTargetFolder().getId(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
