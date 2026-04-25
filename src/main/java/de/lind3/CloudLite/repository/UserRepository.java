package de.lind3.CloudLite.repository;

import de.lind3.CloudLite.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySubject(String subject);
}
