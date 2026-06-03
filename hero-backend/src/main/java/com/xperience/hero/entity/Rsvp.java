package com.xperience.hero.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "rsvps", indexes = {
        @Index(name = "idx_rsvps_event_status", columnList = "event_id,status")
})
@Getter
@Setter
@NoArgsConstructor
public class Rsvp {

    @Id
    @Column(name = "invitation_id")
    private Long invitationId;

    @Column(name = "event_id", nullable = false)
    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RsvpStatus status;

    @Column(name = "yes_at")
    private Instant yesAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }
}
