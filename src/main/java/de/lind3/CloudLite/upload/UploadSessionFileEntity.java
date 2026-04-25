package de.lind3.CloudLite.upload;

import de.lind3.CloudLite.blob.BlobEntity;
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
 * The {@link BlobEntity} link is set once the binary content has been streamed to storage;
 * status advances through PENDING → UPLOADED → COMMITTED (or FAILED).
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

    /**
     * Set once the blob has been written to the storage backend.
     * Null while the file is still being streamed (PENDING).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blob_id")
    private BlobEntity blob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadSessionFileStatus status = UploadSessionFileStatus.PENDING;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
