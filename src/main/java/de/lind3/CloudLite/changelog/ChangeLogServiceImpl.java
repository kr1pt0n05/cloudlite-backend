package de.lind3.CloudLite.changelog;

import de.lind3.CloudLite.file.FileEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeLogServiceImpl implements ChangeLogService {

    private final ChangeLogRepository changeLogRepository;

    @Override
    @Transactional
    public ChangeLogEntity logChange(
            EventType eventType,
            EntityType entityType,
            String path,
            String newPath,
            FileEntity file
    ) {
        ChangeLogEntity changeLog = new ChangeLogEntity();
        changeLog.setEventType(eventType);
        changeLog.setEntityType(entityType);
        changeLog.setTimestamp(Instant.now());
        changeLog.setPath(path);
        changeLog.setNewPath(newPath);
        changeLog.setFile(file);

        return changeLogRepository.save(changeLog);
    }

    @Override
    @Transactional
    public List<ChangeLogEntity> logChanges(List<ChangeLogEntity> changeLogs) {
        Instant timestamp = Instant.now();
        changeLogs.stream()
                .filter(changeLog -> changeLog.getTimestamp() == null)
                .forEach(changeLog -> changeLog.setTimestamp(timestamp));

        return changeLogRepository.saveAll(changeLogs);
    }
}
