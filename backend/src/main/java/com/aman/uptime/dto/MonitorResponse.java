package com.aman.uptime.dto;

import com.aman.uptime.entity.MonitorStatus;

import java.time.Instant;

public record MonitorResponse(
        Long id,
        String name,
        String url,
        String httpMethod,
        int intervalSeconds,
        int timeoutSeconds,
        int expectedStatus,
        boolean active,
        String webhookUrl,
        String alertEmail,
        MonitorStatus status,
        Instant lastCheckedAt,
        Long lastResponseTimeMs,
        Double uptime24h,
        Instant createdAt) {
}
