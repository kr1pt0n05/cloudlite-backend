package de.lind3.CloudLite.entity;

import de.lind3.CloudLite.entity.enums.UploadSessionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Groups one or more file uploads into a single atomic operation.
 * Files staged in the session are not visible to other users until the session
 * transitions to {@link UploadSessionStatus#COMMITTED}.
 */
@Entity
@Table(name = "upload_sessions")
@Getter
@Setter
@NoArgsConstructor
public class UploadSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private UserEntity owner;

    /**
     * The folder in which all files in this session will be published upon commit.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_folder_id", nullable = false)
    private FolderEntity targetFolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadSessionStatus status = UploadSessionStatus.OPEN;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private Instant updatedAt;
}
