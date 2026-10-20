package com.aman.uptime.controller;

import com.aman.uptime.dto.CheckResultResponse;
import com.aman.uptime.dto.IncidentResponse;
import com.aman.uptime.dto.MonitorRequest;
import com.aman.uptime.dto.MonitorResponse;
import com.aman.uptime.dto.MonitorStatsResponse;
import com.aman.uptime.security.AppUserDetails;
import com.aman.uptime.service.MonitorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/monitors")
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping
    public List<MonitorResponse> list(@AuthenticationPrincipal AppUserDetails user) {
        return monitorService.list(user.getId());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MonitorResponse create(@AuthenticationPrincipal AppUserDetails user,
                                  @Valid @RequestBody MonitorRequest request) {
        return monitorService.create(user.getId(), request);
    }

    @GetMapping("/{id}")
    public MonitorResponse get(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.get(user.getId(), id);
    }

    @PutMapping("/{id}")
    public MonitorResponse update(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id,
                                  @Valid @RequestBody MonitorRequest request) {
        return monitorService.update(user.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        monitorService.delete(user.getId(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/pause")
    public MonitorResponse pause(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.pause(user.getId(), id);
    }

    @PostMapping("/{id}/resume")
    public MonitorResponse resume(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.resume(user.getId(), id);
    }

    @PostMapping("/{id}/check")
    public MonitorResponse checkNow(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.checkNow(user.getId(), id);
    }

    @GetMapping("/{id}/results")
    public List<CheckResultResponse> results(@AuthenticationPrincipal AppUserDetails user,
                                             @PathVariable Long id,
                                             @RequestParam(defaultValue = "24") int hours) {
        return monitorService.results(user.getId(), id, hours);
    }

    @GetMapping("/{id}/stats")
    public MonitorStatsResponse stats(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.stats(user.getId(), id);
    }

    @GetMapping("/{id}/incidents")
    public List<IncidentResponse> incidents(@AuthenticationPrincipal AppUserDetails user, @PathVariable Long id) {
        return monitorService.incidentsForMonitor(user.getId(), id);
    }
}
