package de.lind3.CloudLite.entity;

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
 * The actual content is stored via a {@link FileVersionEntity} → {@link BlobEntity} chain.
 * {@code currentVersion} points to the most recently committed version.
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

    /**
     * Points to the latest committed version. Null while the file's initial upload
     * session has not yet been committed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_version_id")
    private FileVersionEntity currentVersion;

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
