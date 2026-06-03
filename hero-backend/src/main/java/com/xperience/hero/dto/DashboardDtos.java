package com.xperience.hero.dto;

import com.xperience.hero.entity.RsvpStatus;

import java.time.Instant;
import java.util.List;

public final class DashboardDtos {
    private DashboardDtos() {}

    public record AttendeeRow(
            Long invitationId,
            String email,
            RsvpStatus status,
            Instant updatedAt
    ) {}

    public record Counts(
            long confirmed,
            long waitlisted,
            long no,
            long maybe,
            long pending,
            Integer capacity
    ) {}

    public record Response(Counts counts, List<AttendeeRow> attendees) {}
}
