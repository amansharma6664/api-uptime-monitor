package com.aman.uptime.service;

import com.aman.uptime.dto.CheckResultResponse;
import com.aman.uptime.dto.DashboardResponse;
import com.aman.uptime.dto.IncidentResponse;
import com.aman.uptime.dto.MonitorRequest;
import com.aman.uptime.dto.MonitorResponse;
import com.aman.uptime.dto.MonitorStatsResponse;
import com.aman.uptime.entity.CheckResult;
import com.aman.uptime.entity.CheckStatus;
import com.aman.uptime.entity.Incident;
import com.aman.uptime.entity.Monitor;
import com.aman.uptime.entity.MonitorStatus;
import com.aman.uptime.exception.BadRequestException;
import com.aman.uptime.exception.ResourceNotFoundException;
import com.aman.uptime.repository.CheckResultRepository;
import com.aman.uptime.repository.IncidentRepository;
import com.aman.uptime.repository.MonitorRepository;
import com.aman.uptime.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class MonitorService {

    private static final int MAX_CHART_POINTS = 300;

    private final MonitorRepository monitorRepository;
    private final UserRepository userRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final MonitorCheckProcessor processor;

    public MonitorService(MonitorRepository monitorRepository,
                          UserRepository userRepository,
                          CheckResultRepository checkResultRepository,
                          IncidentRepository incidentRepository,
                          MonitorCheckProcessor processor) {
        this.monitorRepository = monitorRepository;
        this.userRepository = userRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.processor = processor;
    }

    // ---------------------------------------------------------------- CRUD

    public List<MonitorResponse> list(Long userId) {
        return monitorRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MonitorResponse get(Long userId, Long monitorId) {
        return toResponse(getOwned(userId, monitorId));
    }

    @Transactional
    public MonitorResponse create(Long userId, MonitorRequest request) {
        validateOptionalFields(request);
        Monitor monitor = new Monitor();
        monitor.setOwner(userRepository.getReferenceById(userId));
        apply(monitor, request);
        monitor.setActive(true);
        monitor.setStatus(MonitorStatus.PENDING);
        monitor.setNextCheckAt(Instant.now()); // turant pehla check
        return toResponse(monitorRepository.save(monitor));
    }

    @Transactional
    public MonitorResponse update(Long userId, Long monitorId, MonitorRequest request) {
        validateOptionalFields(request);
        Monitor monitor = getOwned(userId, monitorId);
        apply(monitor, request);
        if (monitor.isActive()) {
            monitor.setNextCheckAt(Instant.now());
        }
        return toResponse(monitorRepository.save(monitor));
    }

    @Transactional
    public void delete(Long userId, Long monitorId) {
        Monitor monitor = getOwned(userId, monitorId);
        checkResultRepository.deleteByMonitorId(monitorId);
        incidentRepository.deleteByMonitorId(monitorId);
        monitorRepository.delete(monitor);
    }

    @Transactional
    public MonitorResponse pause(Long userId, Long monitorId) {
        Monitor monitor = getOwned(userId, monitorId);
        monitor.setActive(false);
        monitor.setStatus(MonitorStatus.PAUSED);
        monitor.setConsecutiveFailures(0);
        // pause karte waqt koi open incident ho to band kar do
        incidentRepository.findFirstByMonitorIdAndResolvedAtIsNull(monitorId)
                .ifPresent(incident -> incident.setResolvedAt(Instant.now()));
        return toResponse(monitorRepository.save(monitor));
    }

    @Transactional
    public MonitorResponse resume(Long userId, Long monitorId) {
        Monitor monitor = getOwned(userId, monitorId);
        monitor.setActive(true);
        monitor.setStatus(MonitorStatus.PENDING);
        monitor.setConsecutiveFailures(0);
        monitor.setNextCheckAt(Instant.now());
        return toResponse(monitorRepository.save(monitor));
    }

    /** "Check now" button: turant ek check chalata hai (blocking, timeout tak). */
    public MonitorResponse checkNow(Long userId, Long monitorId) {
        Monitor monitor = getOwned(userId, monitorId);
        if (!monitor.isActive()) {
            throw new BadRequestException("Monitor is paused. Resume it first.");
        }
        processor.runCheck(monitorId);
        return toResponse(getOwned(userId, monitorId));
    }

    // ---------------------------------------------------------------- data for charts / stats

    public List<CheckResultResponse> results(Long userId, Long monitorId, int hours) {
        getOwned(userId, monitorId);
        int safeHours = Math.min(Math.max(hours, 1), 720);
        Instant since = Instant.now().minus(Duration.ofHours(safeHours));
        List<CheckResult> all =
                checkResultRepository.findByMonitorIdAndCheckedAtAfterOrderByCheckedAtAsc(monitorId, since);

        // Chart halka rakhne ke liye zyada points ho to downsample karo (failed checks hamesha rakho)
        int step = (int) Math.ceil(all.size() / (double) MAX_CHART_POINTS);
        List<CheckResultResponse> out = new ArrayList<>();
        for (int i = 0; i < all.size(); i++) {
            CheckResult r = all.get(i);
            if (step <= 1 || i % step == 0 || r.getStatus() == CheckStatus.DOWN) {
                out.add(new CheckResultResponse(r.getId(), r.getStatus(), r.getStatusCode(),
                        r.getResponseTimeMs(), r.getErrorMessage(), r.getCheckedAt()));
            }
        }
        return out;
    }

    public MonitorStatsResponse stats(Long userId, Long monitorId) {
        getOwned(userId, monitorId);
        Instant since24h = Instant.now().minus(Duration.ofHours(24));
        long total24 = checkResultRepository.countByMonitorIdAndCheckedAtAfter(monitorId, since24h);
        long up24 = checkResultRepository.countByMonitorIdAndCheckedAtAfterAndStatus(monitorId, since24h, CheckStatus.UP);
        Double avg = checkResultRepository.averageResponseTime(monitorId, since24h);
        Double avgRounded = avg == null ? null : (double) Math.round(avg);
        return new MonitorStatsResponse(
                uptime(monitorId, Duration.ofHours(24)),
                uptime(monitorId, Duration.ofDays(7)),
                uptime(monitorId, Duration.ofDays(30)),
                avgRounded,
                total24,
                total24 - up24);
    }

    public List<IncidentResponse> incidentsForMonitor(Long userId, Long monitorId) {
        getOwned(userId, monitorId);
        return incidentRepository.findByMonitor(monitorId, PageRequest.of(0, 50)).stream()
                .map(this::toIncidentResponse)
                .toList();
    }

    public List<IncidentResponse> recentIncidents(Long userId, int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return incidentRepository.findByOwner(userId, PageRequest.of(0, safeLimit)).stream()
                .map(this::toIncidentResponse)
                .toList();
    }

    public DashboardResponse dashboard(Long userId) {
        List<Monitor> monitors = monitorRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        long up = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.UP).count();
        long down = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.DOWN).count();
        long paused = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.PAUSED).count();
        long pending = monitors.stream().filter(m -> m.getStatus() == MonitorStatus.PENDING).count();

        List<Double> uptimes = monitors.stream()
                .map(m -> uptime(m.getId(), Duration.ofHours(24)))
                .filter(Objects::nonNull)
                .toList();
        Double average = null;
        if (!uptimes.isEmpty()) {
            double sum = 0;
            for (Double value : uptimes) {
                sum += value;
            }
            average = Math.round((sum / uptimes.size()) * 100.0) / 100.0;
        }
        return new DashboardResponse(monitors.size(), up, down, paused, pending, average,
                incidentRepository.countOpenByOwner(userId));
    }

    // ---------------------------------------------------------------- helpers

    private Monitor getOwned(Long userId, Long monitorId) {
        return monitorRepository.findByIdAndOwnerId(monitorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Monitor not found"));
    }

    private void apply(Monitor monitor, MonitorRequest request) {
        monitor.setName(request.name().trim());
        monitor.setUrl(request.url().trim());
        monitor.setHttpMethod(request.httpMethod() == null ? "GET" : request.httpMethod());
        monitor.setIntervalSeconds(request.intervalSeconds() == null ? 60 : request.intervalSeconds());
        monitor.setTimeoutSeconds(request.timeoutSeconds() == null ? 10 : request.timeoutSeconds());
        monitor.setExpectedStatus(request.expectedStatus() == null ? 200 : request.expectedStatus());
        monitor.setWebhookUrl(blankToNull(request.webhookUrl()));
        monitor.setAlertEmail(blankToNull(request.alertEmail()));
    }

    private void validateOptionalFields(MonitorRequest request) {
        String webhook = blankToNull(request.webhookUrl());
        if (webhook != null && !webhook.matches("^https?://.+")) {
            throw new BadRequestException("Webhook URL must start with http:// or https://");
        }
        String email = blankToNull(request.alertEmail());
        if (email != null && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new BadRequestException("Alert email is not valid");
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private Double uptime(Long monitorId, Duration window) {
        Instant since = Instant.now().minus(window);
        long total = checkResultRepository.countByMonitorIdAndCheckedAtAfter(monitorId, since);
        long up = checkResultRepository.countByMonitorIdAndCheckedAtAfterAndStatus(monitorId, since, CheckStatus.UP);
        return UptimeCalculator.percentage(up, total);
    }

    private MonitorResponse toResponse(Monitor m) {
        return new MonitorResponse(
                m.getId(), m.getName(), m.getUrl(), m.getHttpMethod(),
                m.getIntervalSeconds(), m.getTimeoutSeconds(), m.getExpectedStatus(),
                m.isActive(), m.getWebhookUrl(), m.getAlertEmail(),
                m.getStatus(), m.getLastCheckedAt(), m.getLastResponseTimeMs(),
                uptime(m.getId(), Duration.ofHours(24)), m.getCreatedAt());
    }

    private IncidentResponse toIncidentResponse(Incident i) {
        Instant end = i.getResolvedAt() != null ? i.getResolvedAt() : Instant.now();
        return new IncidentResponse(
                i.getId(), i.getMonitor().getId(), i.getMonitor().getName(),
                i.getStartedAt(), i.getResolvedAt(),
                Duration.between(i.getStartedAt(), end).getSeconds(),
                i.getResolvedAt() == null, i.getReason());
    }
}
