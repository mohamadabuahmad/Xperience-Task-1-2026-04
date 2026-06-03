package com.xperience.hero.controller;

import com.xperience.hero.auth.SessionKeys;
import com.xperience.hero.dto.EventDtos;
import com.xperience.hero.entity.Event;
import com.xperience.hero.exception.UnauthorizedException;
import com.xperience.hero.service.EventService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/host/events")
public class EventController {

    private final EventService events;

    public EventController(EventService events) {
        this.events = events;
    }

    private Long hostId(HttpSession session) {
        Long id = (Long) session.getAttribute(SessionKeys.HOST_ID);
        if (id == null) throw new UnauthorizedException("Not logged in");
        return id;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtos.Response create(@Valid @RequestBody EventDtos.CreateRequest req, HttpSession session) {
        Event e = events.create(hostId(session), req);
        return EventDtos.Response.from(e, EventService.isLocked(e));
    }

    @GetMapping
    public List<EventDtos.Response> list(HttpSession session) {
        return events.listForHost(hostId(session)).stream()
                .map(e -> EventDtos.Response.from(e, EventService.isLocked(e)))
                .toList();
    }

    @GetMapping("/{id}")
    public EventDtos.Response get(@PathVariable Long id, HttpSession session) {
        Event e = events.getForHost(id, hostId(session));
        return EventDtos.Response.from(e, EventService.isLocked(e));
    }

    @PostMapping("/{id}/close")
    public EventDtos.Response close(@PathVariable Long id, HttpSession session) {
        Event e = events.close(id, hostId(session));
        return EventDtos.Response.from(e, EventService.isLocked(e));
    }

    @PostMapping("/{id}/cancel")
    public EventDtos.Response cancel(@PathVariable Long id, HttpSession session) {
        Event e = events.cancel(id, hostId(session));
        return EventDtos.Response.from(e, EventService.isLocked(e));
    }
}
