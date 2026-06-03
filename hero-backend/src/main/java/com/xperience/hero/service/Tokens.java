package com.xperience.hero.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

public final class Tokens {

    private static final SecureRandom RNG = new SecureRandom();
    private static final Base64.Encoder URL_ENC = Base64.getUrlEncoder().withoutPadding();

    private Tokens() {}

    /** 256-bit random URL-safe token. Shown to invitee, never stored in DB. */
    public static String mint() {
        byte[] bytes = new byte[32];
        RNG.nextBytes(bytes);
        return URL_ENC.encodeToString(bytes);
    }

    /** SHA-256 hex; deterministic so we can look up by token. */
    public static String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(token.getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
