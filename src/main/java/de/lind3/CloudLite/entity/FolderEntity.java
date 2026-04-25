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
 * Represents a directory (folder) in the hierarchical virtual filesystem.
 * The root of a user's storage has a null parent.
 */
@Entity
@Table(
    name = "folders",
    uniqueConstraints = @UniqueConstraint(columnNames = {"parent_id", "owner_id", "name"})
)
@Getter
@Setter
@NoArgsConstructor
public class FolderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    /** Null for root-level folders. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private FolderEntity parent;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;

    /** Non-null when the folder has been soft-deleted. */
    @Column
    private Instant deletedAt;
}
