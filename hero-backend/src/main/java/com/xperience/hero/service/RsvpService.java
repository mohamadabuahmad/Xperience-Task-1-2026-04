package com.xperience.hero.service;

import com.xperience.hero.dto.RsvpDtos;
import com.xperience.hero.entity.Event;
import com.xperience.hero.entity.EventState;
import com.xperience.hero.entity.Invitation;
import com.xperience.hero.entity.Rsvp;
import com.xperience.hero.entity.RsvpStatus;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.repository.EventRepository;
import com.xperience.hero.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class RsvpService {

    private final EventRepository events;
    private final RsvpRepository rsvps;
    private final InvitationService invitations;

    public RsvpService(EventRepository events, RsvpRepository rsvps, InvitationService invitations) {
        this.events = events;
        this.rsvps = rsvps;
        this.invitations = invitations;
    }

    @Transactional(readOnly = true)
    public RsvpDtos.InviteeView view(String token) {
        Invitation inv = invitations.requireByToken(token);
        Event e = events.findById(inv.getEventId())
                .orElseThrow(() -> new NotFoundException("Event missing"));
        RsvpStatus status = rsvps.findById(inv.getId()).map(Rsvp::getStatus).orElse(null);
        return new RsvpDtos.InviteeView(
                e.getTitle(), e.getDescription(), e.getLocation(),
                e.getStartTime(), e.getState(), EventService.isLocked(e),
                inv.getEmail(), status
        );
    }

    /**
     * Single capacity-affecting pathway (Step 12). Locks the event row,
     * decides confirmed vs waitlisted, and promotes the oldest waitlisted
     * entry if a confirmed seat just freed up.
     */
    @Transactional
    public RsvpDtos.InviteeView submit(String token, RsvpDtos.Choice choice) {
        Invitation inv = invitations.requireByToken(token);

        Event e = events.findByIdForUpdate(inv.getEventId())
                .orElseThrow(() -> new NotFoundException("Event missing"));

        if (e.getState() == EventState.CANCELLED) {
            throw new ConflictException("Event has been cancelled");
        }
        if (EventService.isLocked(e)) {
            throw new ConflictException("Event has already started; RSVPs are locked");
        }

        Rsvp existing = rsvps.findById(inv.getId()).orElse(null);
        RsvpStatus previous = existing == null ? null : existing.getStatus();

        RsvpStatus next = switch (choice) {
            case NO -> RsvpStatus.NO;
            case MAYBE -> RsvpStatus.MAYBE;
            case YES -> decideYes(e, previous);
        };

        Rsvp toSave = existing == null ? new Rsvp() : existing;
        if (existing == null) {
            toSave.setInvitationId(inv.getId());
            toSave.setEventId(e.getId());
        }
        toSave.setStatus(next);
        if (next == RsvpStatus.YES_CONFIRMED || next == RsvpStatus.YES_WAITLISTED) {
            if (toSave.getYesAt() == null) toSave.setYesAt(Instant.now());
        } else {
            toSave.setYesAt(null);
        }
        rsvps.save(toSave);

        // Promotion: a confirmed seat just freed iff previous was YES_CONFIRMED and next is not.
        if (previous == RsvpStatus.YES_CONFIRMED && next != RsvpStatus.YES_CONFIRMED) {
            promoteOneIfPossible(e);
        }

        return view(token);
    }

    private RsvpStatus decideYes(Event e, RsvpStatus previous) {
        if (previous == RsvpStatus.YES_CONFIRMED) return RsvpStatus.YES_CONFIRMED;
        Integer cap = e.getCapacity();
        if (cap == null) return RsvpStatus.YES_CONFIRMED;
        long confirmed = rsvps.countByEventIdAndStatus(e.getId(), RsvpStatus.YES_CONFIRMED);
        // If the invitee was already waitlisted, freeing their own slot doesn't apply;
        // they're moving within YES_*, not from confirmed.
        return confirmed < cap ? RsvpStatus.YES_CONFIRMED : RsvpStatus.YES_WAITLISTED;
    }

    private void promoteOneIfPossible(Event e) {
        Integer cap = e.getCapacity();
        if (cap == null) return; // no cap → no waitlist concept
        long confirmed = rsvps.countByEventIdAndStatus(e.getId(), RsvpStatus.YES_CONFIRMED);
        if (confirmed >= cap) return;
        rsvps.findFirstByEventIdAndStatusOrderByYesAtAsc(e.getId(), RsvpStatus.YES_WAITLISTED)
                .ifPresent(top -> top.setStatus(RsvpStatus.YES_CONFIRMED));
    }
}
