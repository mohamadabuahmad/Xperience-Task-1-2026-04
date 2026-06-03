package com.xperience.hero.dto;

import com.xperience.hero.entity.EventState;
import com.xperience.hero.entity.RsvpStatus;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public final class RsvpDtos {
    private RsvpDtos() {}

    /** Invitee submits one of YES, NO, MAYBE. Server decides confirmed vs waitlisted. */
    public enum Choice { YES, NO, MAYBE }

    public record SubmitRequest(@NotNull Choice choice) {}

    /** What the invitee sees when they open their link. */
    public record InviteeView(
            String eventTitle,
            String eventDescription,
            String eventLocation,
            Instant eventStartTime,
            EventState eventState,
            boolean locked,
            String invitedEmail,
            RsvpStatus currentStatus
    ) {}
}
