package com.xperience.hero.controller;

import com.xperience.hero.auth.SessionKeys;
import com.xperience.hero.dto.AuthDtos;
import com.xperience.hero.entity.Host;
import com.xperience.hero.exception.UnauthorizedException;
import com.xperience.hero.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/signup")
    public AuthDtos.AuthResponse signup(@Valid @RequestBody AuthDtos.SignupRequest req, HttpSession session) {
        Host h = auth.signup(req);
        session.setAttribute(SessionKeys.HOST_ID, h.getId());
        session.setAttribute(SessionKeys.HOST_EMAIL, h.getEmail());
        return new AuthDtos.AuthResponse(h.getId(), h.getEmail());
    }

    @PostMapping("/login")
    public AuthDtos.AuthResponse login(@Valid @RequestBody AuthDtos.LoginRequest req, HttpSession session) {
        Host h = auth.login(req);
        session.setAttribute(SessionKeys.HOST_ID, h.getId());
        session.setAttribute(SessionKeys.HOST_EMAIL, h.getEmail());
        return new AuthDtos.AuthResponse(h.getId(), h.getEmail());
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public AuthDtos.AuthResponse me(HttpSession session) {
        Long id = (Long) session.getAttribute(SessionKeys.HOST_ID);
        String email = (String) session.getAttribute(SessionKeys.HOST_EMAIL);
        if (id == null) throw new UnauthorizedException("Not logged in");
        return new AuthDtos.AuthResponse(id, email);
    }
}
