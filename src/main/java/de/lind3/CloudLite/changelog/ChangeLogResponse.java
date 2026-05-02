package de.lind3.CloudLite.changelog;

import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for a change log entry.
 */
public record ChangeLogResponse(
        Long id,
        EventType eventType,
        EntityType entityType,
        Instant timestamp,
        UUID fileId,
        UUID folderId,
        UUID userId
) {
    public static ChangeLogResponse from(ChangeLogEntity entity) {
        return new ChangeLogResponse(
                entity.getId(),
                entity.getEventType(),
                entity.getEntityType(),
                entity.getTimestamp(),
                entity.getFile() != null ? entity.getFile().getId() : null,
                entity.getFolder() != null ? entity.getFolder().getId() : null,
                entity.getUser() != null ? entity.getUser().getId() : null
        );
    }
}
