package com.aman.uptime.controller;

import com.aman.uptime.dto.AdminOverviewResponse;
import com.aman.uptime.repository.CheckResultRepository;
import com.aman.uptime.repository.IncidentRepository;
import com.aman.uptime.repository.MonitorRepository;
import com.aman.uptime.repository.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Sirf ROLE_ADMIN ke liye (SecurityConfig me /api/admin/** protected hai). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;

    public AdminController(UserRepository userRepository, MonitorRepository monitorRepository,
                           CheckResultRepository checkResultRepository, IncidentRepository incidentRepository) {
        this.userRepository = userRepository;
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return new AdminOverviewResponse(
                userRepository.count(),
                monitorRepository.count(),
                checkResultRepository.count(),
                incidentRepository.count());
    }
}
