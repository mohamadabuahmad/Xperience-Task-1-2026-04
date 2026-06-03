package com.xperience.hero.controller;

import com.xperience.hero.auth.SessionKeys;
import com.xperience.hero.dto.InvitationDtos;
import com.xperience.hero.exception.UnauthorizedException;
import com.xperience.hero.service.InvitationService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/host/events/{eventId}/invitations")
public class InvitationController {

    private final InvitationService invitations;

    public InvitationController(InvitationService invitations) {
        this.invitations = invitations;
    }

    private Long hostId(HttpSession session) {
        Long id = (Long) session.getAttribute(SessionKeys.HOST_ID);
        if (id == null) throw new UnauthorizedException("Not logged in");
        return id;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationDtos.CreatedResponse invite(
            @PathVariable Long eventId,
            @Valid @RequestBody InvitationDtos.CreateRequest req,
            HttpSession session
    ) {
        return invitations.invite(eventId, hostId(session), req.email());
    }

    @GetMapping
    public List<InvitationDtos.ListItem> list(@PathVariable Long eventId, HttpSession session) {
        return invitations.listForEvent(eventId, hostId(session));
    }
}
