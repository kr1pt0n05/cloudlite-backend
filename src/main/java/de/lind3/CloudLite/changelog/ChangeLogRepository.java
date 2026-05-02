package de.lind3.CloudLite.changelog;

import de.lind3.CloudLite.user.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChangeLogRepository extends JpaRepository<ChangeLogEntity, Long> {

    @EntityGraph(attributePaths = {"file", "folder", "user"})
    List<ChangeLogEntity> findByUserAndIdGreaterThanOrderByIdAsc(UserEntity user, Long id);
}
