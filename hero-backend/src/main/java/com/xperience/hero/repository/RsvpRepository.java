package com.xperience.hero.repository;

import com.xperience.hero.entity.Rsvp;
import com.xperience.hero.entity.RsvpStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RsvpRepository extends JpaRepository<Rsvp, Long> {

    long countByEventIdAndStatus(Long eventId, RsvpStatus status);

    List<Rsvp> findByEventId(Long eventId);

    Optional<Rsvp> findFirstByEventIdAndStatusOrderByYesAtAsc(Long eventId, RsvpStatus status);
}
