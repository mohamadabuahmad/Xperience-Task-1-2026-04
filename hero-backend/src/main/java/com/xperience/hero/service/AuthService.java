package com.xperience.hero.service;

import com.xperience.hero.dto.AuthDtos;
import com.xperience.hero.entity.Host;
import com.xperience.hero.exception.ConflictException;
import com.xperience.hero.exception.UnauthorizedException;
import com.xperience.hero.repository.HostRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final HostRepository hosts;
    private final PasswordEncoder encoder;

    public AuthService(HostRepository hosts, PasswordEncoder encoder) {
        this.hosts = hosts;
        this.encoder = encoder;
    }

    @Transactional
    public Host signup(AuthDtos.SignupRequest req) {
        String email = req.email().toLowerCase();
        if (hosts.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
        Host h = new Host();
        h.setEmail(email);
        h.setPasswordHash(encoder.encode(req.password()));
        return hosts.save(h);
    }

    @Transactional(readOnly = true)
    public Host login(AuthDtos.LoginRequest req) {
        Host h = hosts.findByEmail(req.email().toLowerCase())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        if (!encoder.matches(req.password(), h.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }
        return h;
    }
}
