package de.lind3.CloudLite.changelog;

import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.user.UserEntity;

import java.util.List;

/**
 * Records file and directory change events in an append-only log table.
 */
public interface ChangeLogService {

    /**
     * Persists a single change log row.
     *
     * @param eventType  action that happened
     * @param entityType changed entity type
     * @param file       changed file reference, otherwise {@code null}
     * @param folder     changed folder reference, otherwise {@code null}
     * @param user       user that performed the change
     * @return persisted change log entity
     */
    ChangeLogEntity logChange(
            EventType eventType,
            EntityType entityType,
            FileEntity file,
            FolderEntity folder,
            UserEntity user
    );

    /**
     * Persists multiple change log rows.
     *
     * @param changeLogs log rows to persist
     * @return persisted change log entities
     */
    List<ChangeLogEntity> logChanges(List<ChangeLogEntity> changeLogs);

    /**
     * Returns all change log rows for the requesting user after the given log ID.
     *
     * @param latestSyncedId   last log ID already known to the client
     * @param requesterSubject JWT {@code sub} claim of the requesting user
     * @return user-scoped change log rows ordered by ID ascending
     */
    List<ChangeLogEntity> getChangesSince(Long latestSyncedId, String requesterSubject);
}
