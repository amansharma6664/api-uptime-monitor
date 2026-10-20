package com.aman.uptime.repository;

import com.aman.uptime.entity.Monitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MonitorRepository extends JpaRepository<Monitor, Long> {

    List<Monitor> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Monitor> findByIdAndOwnerId(Long id, Long ownerId);

    long countByOwnerId(Long ownerId);

    /** Jin monitors ka check abhi due hai (active + nextCheckAt nikal chuka). */
    @Query("select m from Monitor m where m.active = true and (m.nextCheckAt is null or m.nextCheckAt <= :now)")
    List<Monitor> findDue(@Param("now") Instant now);

    /** Owner ke saath load karta hai (alert email ke liye chahiye). */
    @Query("select m from Monitor m join fetch m.owner where m.id = :id")
    Optional<Monitor> findWithOwnerById(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("update Monitor m set m.nextCheckAt = :next where m.id = :id")
    void updateNextCheck(@Param("id") Long id, @Param("next") Instant next);
}
