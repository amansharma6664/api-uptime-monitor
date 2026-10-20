package com.aman.uptime.dto;

import java.time.Instant;

public record IncidentResponse(
        Long id,
        Long monitorId,
        String monitorName,
        Instant startedAt,
        Instant resolvedAt,
        long durationSeconds,
        boolean ongoing,
        String reason) {
}
