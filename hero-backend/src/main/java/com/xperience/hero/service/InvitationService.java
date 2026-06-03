package com.xperience.hero.service;

import com.xperience.hero.dto.InvitationDtos;
import com.xperience.hero.entity.Event;
import com.xperience.hero.entity.EventState;
import com.xperience.hero.entity.Invitation;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.repository.InvitationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvitationService {

    private final InvitationRepository invitations;
    private final EventService eventService;
    private final String inviteBaseUrl;

    public InvitationService(
            InvitationRepository invitations,
            EventService eventService,
            @Value("${app.invite.base-url}") String inviteBaseUrl
    ) {
        this.invitations = invitations;
        this.eventService = eventService;
        this.inviteBaseUrl = inviteBaseUrl;
    }

    @Transactional
    public InvitationDtos.CreatedResponse invite(Long eventId, Long hostId, String rawEmail) {
        Event e = eventService.getForHost(eventId, hostId);
        if (e.getState() == EventState.CANCELLED) {
            throw new ConflictException("Event is cancelled");
        }
        if (e.getState() == EventState.CLOSED) {
            throw new ConflictException("Event is closed to new invitees");
        }
        if (EventService.isLocked(e)) {
            throw new ConflictException("Event has already started");
        }
        String email = rawEmail.toLowerCase();
        if (invitations.existsByEventIdAndEmail(eventId, email)) {
            throw new ConflictException("Already invited");
        }
        String token = Tokens.mint();
        Invitation inv = new Invitation();
        inv.setEventId(eventId);
        inv.setEmail(email);
        inv.setTokenHash(Tokens.hash(token));
        Invitation saved = invitations.save(inv);
        return new InvitationDtos.CreatedResponse(
                saved.getId(), saved.getEmail(), token,
                inviteBaseUrl + "/" + token, saved.getIssuedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<InvitationDtos.ListItem> listForEvent(Long eventId, Long hostId) {
        eventService.getForHost(eventId, hostId);
        return invitations.findByEventIdOrderByIssuedAtAsc(eventId).stream()
                .map(i -> new InvitationDtos.ListItem(i.getId(), i.getEmail(), i.getIssuedAt()))
                .toList();
    }

    public Invitation requireByToken(String token) {
        return invitations.findByTokenHash(Tokens.hash(token))
                .orElseThrow(() -> new NotFoundException("Invalid invitation link"));
    }
}
