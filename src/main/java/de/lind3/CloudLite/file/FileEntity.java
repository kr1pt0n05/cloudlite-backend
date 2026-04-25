package de.lind3.CloudLite.file;

import de.lind3.CloudLite.blob.BlobEntity;
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
 * The binary content is stored in the linked {@link BlobEntity}.
 * Null while the file's upload session has not yet been committed.
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
     * The blob holding the file's binary content.
     * Null while the file's upload session has not yet been committed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blob_id")
    private BlobEntity blob;

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
