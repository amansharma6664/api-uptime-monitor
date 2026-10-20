package com.aman.uptime.repository;

import com.aman.uptime.entity.CheckResult;
import com.aman.uptime.entity.CheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

public interface CheckResultRepository extends JpaRepository<CheckResult, Long> {

    List<CheckResult> findByMonitorIdAndCheckedAtAfterOrderByCheckedAtAsc(Long monitorId, Instant since);

    long countByMonitorIdAndCheckedAtAfter(Long monitorId, Instant since);

    long countByMonitorIdAndCheckedAtAfterAndStatus(Long monitorId, Instant since, CheckStatus status);

    @Query("select avg(c.responseTimeMs) from CheckResult c "
            + "where c.monitor.id = :monitorId and c.checkedAt > :since and c.status = com.aman.uptime.entity.CheckStatus.UP")
    Double averageResponseTime(@Param("monitorId") Long monitorId, @Param("since") Instant since);

    @Transactional
    @Modifying
    @Query("delete from CheckResult c where c.checkedAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Transactional
    @Modifying
    @Query("delete from CheckResult c where c.monitor.id = :monitorId")
    void deleteByMonitorId(@Param("monitorId") Long monitorId);
}
