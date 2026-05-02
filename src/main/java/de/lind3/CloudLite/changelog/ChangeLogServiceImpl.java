package de.lind3.CloudLite.changelog;

import de.lind3.CloudLite.file.FileEntity;
import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.user.UserEntity;
import de.lind3.CloudLite.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChangeLogServiceImpl implements ChangeLogService {

    private final ChangeLogRepository changeLogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChangeLogEntity logChange(
            EventType eventType,
            EntityType entityType,
            FileEntity file,
            FolderEntity folder,
            UserEntity user
    ) {
        ChangeLogEntity changeLog = new ChangeLogEntity();
        changeLog.setEventType(eventType);
        changeLog.setEntityType(entityType);
        changeLog.setTimestamp(Instant.now());
        changeLog.setFile(file);
        changeLog.setFolder(folder);
        changeLog.setUser(user);

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

    @Override
    @Transactional(readOnly = true)
    public List<ChangeLogEntity> getChangesSince(Long latestSyncedId, String requesterSubject) {
        if (latestSyncedId == null || latestSyncedId < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "latestSyncedId must be zero or greater");
        }

        return userRepository.findBySubject(requesterSubject)
                .map(user -> changeLogRepository.findByUserAndIdGreaterThanOrderByIdAsc(user, latestSyncedId))
                .orElseGet(List::of);
    }
}
