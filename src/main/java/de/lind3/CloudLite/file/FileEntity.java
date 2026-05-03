package de.lind3.CloudLite.file;

import de.lind3.CloudLite.folder.FolderEntity;
import de.lind3.CloudLite.user.UserEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a logical file entry inside a folder.
 * The binary content is stored directly on the configured filesystem path.
 */
@Entity
@Table(
    name = "files",
    uniqueConstraints = @UniqueConstraint(columnNames = {"folder_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "folder_id", nullable = false)
    private FolderEntity folder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    /** Relative path below the configured storage base path. */
    @Column(name = "storage_path", nullable = false, unique = true)
    private String storagePath;

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

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Non-null when the file has been soft-deleted. */
    @Column
    private Instant deletedAt;
}
