package com.xperience.hero.controller;

import com.xperience.hero.auth.SessionKeys;
import com.xperience.hero.dto.DashboardDtos;
import com.xperience.hero.exception.UnauthorizedException;
import com.xperience.hero.service.DashboardService;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/host/events/{eventId}/dashboard")
public class DashboardController {

    private final DashboardService dashboard;

    public DashboardController(DashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping
    public DashboardDtos.Response get(@PathVariable Long eventId, HttpSession session) {
        Long hostId = (Long) session.getAttribute(SessionKeys.HOST_ID);
        if (hostId == null) throw new UnauthorizedException("Not logged in");
        return dashboard.forEvent(eventId, hostId);
    }
}
