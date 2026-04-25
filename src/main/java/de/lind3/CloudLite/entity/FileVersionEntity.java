package de.lind3.CloudLite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Maps a {@link FileEntity} to a specific {@link BlobEntity} at a given version number.
 * Versions are monotonically increasing per file.
 */
@Entity
@Table(
    name = "file_versions",
    uniqueConstraints = @UniqueConstraint(columnNames = {"file_id", "version_number"})
)
@Getter
@Setter
@NoArgsConstructor
public class FileVersionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blob_id", nullable = false)
    private BlobEntity blob;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
