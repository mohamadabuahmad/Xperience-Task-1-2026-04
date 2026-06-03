package com.xperience.hero.service;

import com.xperience.hero.dto.EventDtos;
import com.xperience.hero.entity.Event;
import com.xperience.hero.entity.EventState;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.ForbiddenException;
import com.xperience.hero.exception.NotFoundException;
import com.xperience.hero.repository.EventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class EventService {

    private final EventRepository events;

    public EventService(EventRepository events) {
        this.events = events;
    }

    @Transactional
    public Event create(Long hostId, EventDtos.CreateRequest req) {
        if (!req.startTime().isAfter(Instant.now())) {
            throw new ConflictException("start_time must be in the future");
        }
        Event e = new Event();
        e.setHostId(hostId);
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setStartTime(req.startTime());
        e.setLocation(req.location());
        e.setCapacity(req.capacity());
        e.setState(EventState.OPEN);
        return events.save(e);
    }

    @Transactional(readOnly = true)
    public List<Event> listForHost(Long hostId) {
        return events.findByHostIdOrderByStartTimeDesc(hostId);
    }

    @Transactional(readOnly = true)
    public Event getForHost(Long eventId, Long hostId) {
        Event e = events.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event not found"));
        if (!e.getHostId().equals(hostId)) {
            throw new ForbiddenException("Not your event");
        }
        return e;
    }

    @Transactional
    public Event close(Long eventId, Long hostId) {
        Event e = getForHost(eventId, hostId);
        if (e.getState() == EventState.CANCELLED) {
            throw new ConflictException("Event already cancelled");
        }
        e.setState(EventState.CLOSED);
        return e;
    }

    @Transactional
    public Event cancel(Long eventId, Long hostId) {
        Event e = getForHost(eventId, hostId);
        if (e.getState() == EventState.CANCELLED) {
            throw new ConflictException("Event already cancelled");
        }
        e.setState(EventState.CANCELLED);
        return e;
    }

    /** I-2 — locked iff event start time has passed OR event is cancelled. */
    public static boolean isLocked(Event e) {
        if (e.getState() == EventState.CANCELLED) return true;
        return !Instant.now().isBefore(e.getStartTime());
    }
}
