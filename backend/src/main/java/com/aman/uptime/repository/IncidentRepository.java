package com.aman.uptime.repository;

import com.aman.uptime.entity.Incident;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

    Optional<Incident> findFirstByMonitorIdAndResolvedAtIsNull(Long monitorId);

    @Query("select i from Incident i join fetch i.monitor where i.monitor.id = :monitorId order by i.startedAt desc")
    List<Incident> findByMonitor(@Param("monitorId") Long monitorId, Pageable pageable);

    @Query("select i from Incident i join fetch i.monitor m where m.owner.id = :ownerId order by i.startedAt desc")
    List<Incident> findByOwner(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("select count(i) from Incident i where i.monitor.owner.id = :ownerId and i.resolvedAt is null")
    long countOpenByOwner(@Param("ownerId") Long ownerId);

    @Transactional
    @Modifying
    @Query("delete from Incident i where i.monitor.id = :monitorId")
    void deleteByMonitorId(@Param("monitorId") Long monitorId);
}
