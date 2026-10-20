package com.aman.uptime.service;

import com.aman.uptime.entity.CheckResult;
import com.aman.uptime.entity.CheckStatus;
import com.aman.uptime.entity.Incident;
import com.aman.uptime.entity.Monitor;
import com.aman.uptime.entity.MonitorStatus;
import com.aman.uptime.repository.CheckResultRepository;
import com.aman.uptime.repository.IncidentRepository;
import com.aman.uptime.repository.MonitorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Ek monitor ka ek check chalata hai:
 *   1) HTTP call (transaction ke bahar, taaki DB connection block na ho)
 *   2) result save + status / incident update (chhoti transaction me)
 *   3) transaction commit hone ke baad alert bhejta hai
 *
 * Flapping se bachne ke liye monitor tab DOWN mark hota hai jab lagatar
 * `app.monitor.failure-threshold` (default 2) checks fail ho jayein.
 */
@Service
public class MonitorCheckProcessor {

    private static final Logger log = LoggerFactory.getLogger(MonitorCheckProcessor.class);

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final IncidentRepository incidentRepository;
    private final HttpCheckService httpCheckService;
    private final AlertService alertService;
    private final TransactionTemplate transactionTemplate;
    private final int failureThreshold;

    public MonitorCheckProcessor(MonitorRepository monitorRepository,
                                 CheckResultRepository checkResultRepository,
                                 IncidentRepository incidentRepository,
                                 HttpCheckService httpCheckService,
                                 AlertService alertService,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${app.monitor.failure-threshold:2}") int failureThreshold) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.incidentRepository = incidentRepository;
        this.httpCheckService = httpCheckService;
        this.alertService = alertService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.failureThreshold = Math.max(1, failureThreshold);
    }

    public void runCheck(Long monitorId) {
        Monitor snapshot = monitorRepository.findById(monitorId).orElse(null);
        if (snapshot == null || !snapshot.isActive()) {
            return; // delete ya pause ho chuka hai
        }

        CheckOutcome outcome = httpCheckService.check(
                snapshot.getUrl(), snapshot.getHttpMethod(), snapshot.getTimeoutSeconds(), snapshot.getExpectedStatus());

        AlertMessage alert;
        try {
            alert = transactionTemplate.execute(status -> record(monitorId, outcome));
        } catch (Exception ex) {
            log.error("Could not save check result for monitor {}", monitorId, ex);
            return;
        }

        if (alert != null) {
            alertService.send(alert);
        }
    }

    /** Transaction ke andar chalta hai. Alert bhejna ho to AlertMessage return karta hai. */
    private AlertMessage record(Long monitorId, CheckOutcome outcome) {
        Monitor monitor = monitorRepository.findWithOwnerById(monitorId).orElse(null);
        if (monitor == null || !monitor.isActive()) {
            return null;
        }
        Instant now = Instant.now();

        CheckResult result = new CheckResult();
        result.setMonitor(monitor);
        result.setStatus(outcome.up() ? CheckStatus.UP : CheckStatus.DOWN);
        result.setStatusCode(outcome.statusCode());
        result.setResponseTimeMs(outcome.responseTimeMs());
        result.setErrorMessage(outcome.error());
        result.setCheckedAt(now);
        checkResultRepository.save(result);

        monitor.setLastCheckedAt(now);
        monitor.setLastResponseTimeMs(outcome.responseTimeMs());

        MonitorStatus previous = monitor.getStatus();
        AlertMessage alert = null;

        if (outcome.up()) {
            monitor.setConsecutiveFailures(0);
            monitor.setStatus(MonitorStatus.UP);
            if (previous == MonitorStatus.DOWN) {
                String detail = "Monitor is back online.";
                Optional<Incident> open = incidentRepository.findFirstByMonitorIdAndResolvedAtIsNull(monitor.getId());
                if (open.isPresent()) {
                    Incident incident = open.get();
                    incident.setResolvedAt(now);
                    long seconds = Duration.between(incident.getStartedAt(), now).getSeconds();
                    detail = "Back online after " + UptimeCalculator.formatDuration(seconds) + " of downtime.";
                }
                alert = buildAlert(AlertMessage.Type.RECOVERED, monitor, detail);
            }
        } else {
            int failures = monitor.getConsecutiveFailures() + 1;
            monitor.setConsecutiveFailures(failures);
            if (failures >= failureThreshold && previous != MonitorStatus.DOWN) {
                monitor.setStatus(MonitorStatus.DOWN);
                Incident incident = new Incident();
                incident.setMonitor(monitor);
                incident.setStartedAt(now);
                incident.setReason(outcome.error());
                incidentRepository.save(incident);
                alert = buildAlert(AlertMessage.Type.DOWN, monitor, outcome.error());
            }
        }
        // monitor "managed" entity hai, isliye changes commit par apne aap save ho jate hain
        return alert;
    }

    private AlertMessage buildAlert(AlertMessage.Type type, Monitor monitor, String detail) {
        String email = (monitor.getAlertEmail() == null || monitor.getAlertEmail().isBlank())
                ? monitor.getOwner().getEmail()
                : monitor.getAlertEmail();
        return new AlertMessage(type, monitor.getName(), monitor.getUrl(), email, monitor.getWebhookUrl(), detail);
    }
}
