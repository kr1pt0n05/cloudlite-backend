package de.lind3.CloudLite.repository;

import de.lind3.CloudLite.entity.FileEntity;
import de.lind3.CloudLite.entity.FileVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FileVersionRepository extends JpaRepository<FileVersionEntity, UUID> {

    List<FileVersionEntity> findByFileOrderByVersionNumberAsc(FileEntity file);

    Optional<FileVersionEntity> findTopByFileOrderByVersionNumberDesc(FileEntity file);

    int countByFile(FileEntity file);
}
