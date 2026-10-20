package com.aman.uptime.scheduler;

import com.aman.uptime.entity.Monitor;
import com.aman.uptime.repository.CheckResultRepository;
import com.aman.uptime.repository.MonitorRepository;
import com.aman.uptime.service.MonitorCheckProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * Heart of the project.
 * Har 5 second me DB se "due" monitors nikalta hai aur har ek ka check
 * thread pool me daal deta hai (parallel + non-blocking).
 */
@Component
public class MonitorScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitorScheduler.class);

    private final MonitorRepository monitorRepository;
    private final CheckResultRepository checkResultRepository;
    private final MonitorCheckProcessor processor;
    private final ExecutorService checkExecutor;
    private final int retentionDays;

    /** Jo monitors abhi check ho rahe hain unka dubara check na lage (overlap se bachne ke liye). */
    private final Set<Long> inFlight = ConcurrentHashMap.newKeySet();

    public MonitorScheduler(MonitorRepository monitorRepository,
                            CheckResultRepository checkResultRepository,
                            MonitorCheckProcessor processor,
                            @Qualifier("checkExecutor") ExecutorService checkExecutor,
                            @Value("${app.monitor.retention-days:30}") int retentionDays) {
        this.monitorRepository = monitorRepository;
        this.checkResultRepository = checkResultRepository;
        this.processor = processor;
        this.checkExecutor = checkExecutor;
        this.retentionDays = retentionDays;
    }

    @Scheduled(fixedDelayString = "${app.monitor.poll-interval-ms:5000}", initialDelay = 5000)
    public void dispatchDueChecks() {
        try {
            Instant now = Instant.now();
            List<Monitor> due = monitorRepository.findDue(now);
            for (Monitor monitor : due) {
                Long id = monitor.getId();
                if (!inFlight.add(id)) {
                    continue; // pichhla check abhi chal raha hai
                }
                monitorRepository.updateNextCheck(id, now.plusSeconds(monitor.getIntervalSeconds()));
                checkExecutor.submit(() -> {
                    try {
                        processor.runCheck(id);
                    } catch (Exception ex) {
                        log.error("Check failed unexpectedly for monitor {}", id, ex);
                    } finally {
                        inFlight.remove(id);
                    }
                });
            }
        } catch (Exception ex) {
            log.error("Scheduler tick failed", ex);
        }
    }

    /** Roz raat 3 baje purane check results delete (default: 30 din se purane). */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldResults() {
        Instant cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deleted = checkResultRepository.deleteOlderThan(cutoff);
        log.info("Cleanup: deleted {} check results older than {} days", deleted, retentionDays);
    }
}
