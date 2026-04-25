package de.lind3.CloudLite.repository;

import de.lind3.CloudLite.entity.UploadSessionEntity;
import de.lind3.CloudLite.entity.UploadSessionFileEntity;
import de.lind3.CloudLite.entity.enums.UploadSessionFileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionFileRepository extends JpaRepository<UploadSessionFileEntity, UUID> {

    List<UploadSessionFileEntity> findBySession(UploadSessionEntity session);

    List<UploadSessionFileEntity> findBySessionAndStatus(UploadSessionEntity session, UploadSessionFileStatus status);

    Optional<UploadSessionFileEntity> findBySessionAndFileName(UploadSessionEntity session, String fileName);

    boolean existsBySessionAndFileName(UploadSessionEntity session, String fileName);
}
