package de.lind3.CloudLite.changelog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangeLogRepository extends JpaRepository<ChangeLogEntity, Long> {
}
