package com.xperience.hero.dto;

import com.xperience.hero.entity.Event;
import com.xperience.hero.entity.EventState;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public final class EventDtos {
    private EventDtos() {}

    public record CreateRequest(
            @NotBlank String title,
            String description,
            @NotNull Instant startTime,
            @NotBlank String location,
            @Positive Integer capacity
    ) {}

    public record Response(
            Long id,
            String title,
            String description,
            Instant startTime,
            String location,
            Integer capacity,
            EventState state,
            boolean locked
    ) {
        public static Response from(Event e, boolean locked) {
            return new Response(
                    e.getId(), e.getTitle(), e.getDescription(),
                    e.getStartTime(), e.getLocation(), e.getCapacity(),
                    e.getState(), locked
            );
        }
    }
}
