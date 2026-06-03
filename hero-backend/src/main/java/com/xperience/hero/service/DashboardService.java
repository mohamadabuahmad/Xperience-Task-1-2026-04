package com.xperience.hero.service;

import com.xperience.hero.dto.DashboardDtos;
import com.xperience.hero.entity.Event;
import com.xperience.hero.entity.Invitation;
import com.xperience.hero.entity.Rsvp;
import com.xperience.hero.entity.RsvpStatus;
import com.xperience.hero.repository.InvitationRepository;
import com.xperience.hero.repository.RsvpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final EventService events;
    private final InvitationRepository invitations;
    private final RsvpRepository rsvps;

    public DashboardService(EventService events, InvitationRepository invitations, RsvpRepository rsvps) {
        this.events = events;
        this.invitations = invitations;
        this.rsvps = rsvps;
    }

    @Transactional(readOnly = true)
    public DashboardDtos.Response forEvent(Long eventId, Long hostId) {
        Event e = events.getForHost(eventId, hostId);
        List<Invitation> invs = invitations.findByEventIdOrderByIssuedAtAsc(eventId);
        Map<Long, Rsvp> byInvId = new HashMap<>();
        for (Rsvp r : rsvps.findByEventId(eventId)) {
            byInvId.put(r.getInvitationId(), r);
        }
        long confirmed = 0, waitlisted = 0, no = 0, maybe = 0, pending = 0;
        var rows = new java.util.ArrayList<DashboardDtos.AttendeeRow>(invs.size());
        for (Invitation i : invs) {
            Rsvp r = byInvId.get(i.getId());
            RsvpStatus s = r == null ? null : r.getStatus();
            if (s == null) pending++;
            else switch (s) {
                case YES_CONFIRMED -> confirmed++;
                case YES_WAITLISTED -> waitlisted++;
                case NO -> no++;
                case MAYBE -> maybe++;
            }
            rows.add(new DashboardDtos.AttendeeRow(
                    i.getId(), i.getEmail(), s,
                    r == null ? null : r.getUpdatedAt()
            ));
        }
        return new DashboardDtos.Response(
                new DashboardDtos.Counts(confirmed, waitlisted, no, maybe, pending, e.getCapacity()),
                rows
        );
    }
}
