package de.lind3.CloudLite.upload;

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
 * Groups one mapped batch upload operation.
 * The session transitions to {@link UploadSessionStatus#COMMITTED} after all
 * files are written to their final filesystem paths and metadata is persisted.
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
