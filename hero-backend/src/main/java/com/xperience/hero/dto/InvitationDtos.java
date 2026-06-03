package com.xperience.hero.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;

public final class InvitationDtos {
    private InvitationDtos() {}

    public record CreateRequest(@Email @NotBlank String email) {}

    /** Returned only on creation: includes the plaintext token + share link. */
    public record CreatedResponse(
            Long id,
            String email,
            String token,
            String link,
            Instant issuedAt
    ) {}

    /** Listing view: never exposes the token. */
    public record ListItem(Long id, String email, Instant issuedAt) {}
}
