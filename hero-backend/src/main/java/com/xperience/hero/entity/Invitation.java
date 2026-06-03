package com.xperience.hero.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "invitations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_invitations_token_hash", columnNames = "token_hash"),
        @UniqueConstraint(name = "uk_invitations_event_email", columnNames = {"event_id", "email"})
}, indexes = {
        @Index(name = "idx_invitations_event", columnList = "event_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Invitation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Column(nullable = false)
    private String email;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @PrePersist
    void onCreate() {
        if (issuedAt == null) issuedAt = Instant.now();
    }
}
