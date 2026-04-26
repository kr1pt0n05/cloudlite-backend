package de.lind3.CloudLite.upload;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single file entry staged inside an {@link UploadSessionEntity}.
 * Blob metadata is stored inline so that {@code BlobEntity} records are only
 * created at commit time, enabling a single batch insert per session commit.
 * Status advances through UPLOADED → COMMITTED (or FAILED).
 */
@Entity
@Table(
    name = "upload_session_files",
    uniqueConstraints = @UniqueConstraint(columnNames = {"session_id", "file_name"})
)
@Getter
@Setter
@NoArgsConstructor
public class UploadSessionFileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private UploadSessionEntity session;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    /** Opaque key used to locate the blob in the storage backend. */
    @Column(name = "storage_key", nullable = false)
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadSessionFileStatus status = UploadSessionFileStatus.UPLOADED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
