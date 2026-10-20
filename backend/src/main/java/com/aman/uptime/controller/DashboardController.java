package com.aman.uptime.controller;

import com.aman.uptime.dto.DashboardResponse;
import com.aman.uptime.dto.IncidentResponse;
import com.aman.uptime.security.AppUserDetails;
import com.aman.uptime.service.MonitorService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final MonitorService monitorService;

    public DashboardController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AppUserDetails user) {
        return monitorService.dashboard(user.getId());
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> incidents(@AuthenticationPrincipal AppUserDetails user,
                                            @RequestParam(defaultValue = "20") int limit) {
        return monitorService.recentIncidents(user.getId(), limit);
    }
}
