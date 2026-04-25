package de.lind3.CloudLite.blob;

import de.lind3.CloudLite.blob.BlobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlobRepository extends JpaRepository<BlobEntity, UUID> {

    /** Allows deduplication by content hash. */
    Optional<BlobEntity> findBySha256(String sha256);

    Optional<BlobEntity> findByStorageKey(String storageKey);
}
