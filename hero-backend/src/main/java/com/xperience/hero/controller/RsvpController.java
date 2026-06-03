package com.xperience.hero.controller;

import com.xperience.hero.dto.RsvpDtos;
import com.xperience.hero.service.RsvpService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invite/{token}")
public class RsvpController {

    private final RsvpService rsvps;

    public RsvpController(RsvpService rsvps) {
        this.rsvps = rsvps;
    }

    @GetMapping
    public RsvpDtos.InviteeView view(@PathVariable String token) {
        return rsvps.view(token);
    }

    @PostMapping("/rsvp")
    public RsvpDtos.InviteeView submit(@PathVariable String token, @Valid @RequestBody RsvpDtos.SubmitRequest req) {
        return rsvps.submit(token, req.choice());
    }
}
