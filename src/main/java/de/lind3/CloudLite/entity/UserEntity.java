package de.lind3.CloudLite.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a registered user, identified by the Keycloak JWT subject claim.
 */
@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "subject"))
@Getter
@Setter
@NoArgsConstructor
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(updatable = false, nullable = false)
    private UUID id;

    /** Keycloak subject claim (UUID string). */
    @Column(nullable = false, unique = true)
    private String subject;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public UserEntity(String subject) {
        this.subject = subject;
    }
}
