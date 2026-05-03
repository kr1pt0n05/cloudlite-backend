package de.lind3.CloudLite.upload;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for an upload session.
 *
 * @param id             session identifier
 * @param status         current session status
 * @param createdAt      creation timestamp
 */
public record SessionResponse(
        UUID id,
        String status,
        Instant createdAt
) {
    public static SessionResponse from(UploadSessionEntity entity) {
        return new SessionResponse(
                entity.getId(),
                entity.getStatus().name(),
                entity.getCreatedAt()
        );
    }
}
