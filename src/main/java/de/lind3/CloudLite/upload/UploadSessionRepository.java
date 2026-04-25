package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.user.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadSessionRepository extends JpaRepository<UploadSessionEntity, UUID> {

    List<UploadSessionEntity> findByOwnerAndStatus(UserEntity owner, UploadSessionStatus status);

    Optional<UploadSessionEntity> findByIdAndOwner(UUID id, UserEntity owner);
}
