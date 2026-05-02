package de.lind3.CloudLite.changelog;

import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.folder.FolderEntity;

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
     * @return persisted change log entity
     */
    ChangeLogEntity logChange(
            EventType eventType,
            EntityType entityType,
            FileEntity file,
            FolderEntity folder
    );

    /**
     * Persists multiple change log rows.
     *
     * @param changeLogs log rows to persist
     * @return persisted change log entities
     */
    List<ChangeLogEntity> logChanges(List<ChangeLogEntity> changeLogs);
}
