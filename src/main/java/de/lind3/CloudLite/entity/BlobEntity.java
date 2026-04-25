package de.lind3.CloudLite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * An immutable binary blob stored on the underlying storage backend.
 * Blobs are content-addressable: the SHA-256 hash may be used for deduplication.
 */
@Entity
@Table(
    name = "blobs",
    uniqueConstraints = @UniqueConstraint(columnNames = "storage_key")
)
@Getter
@Setter
@NoArgsConstructor
public class BlobEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Opaque key used to locate the blob in the storage backend (e.g., a relative filesystem path). */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    /** Hex-encoded SHA-256 digest of the raw file content. */
    @Column(name = "sha256", length = 64, nullable = false)
    private String sha256;

    /** Exact byte size of the stored content. */
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;

    /** MIME type detected or provided at upload time (e.g., "image/png"). */
    @Column(name = "mime_type")
    private String mimeType;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
