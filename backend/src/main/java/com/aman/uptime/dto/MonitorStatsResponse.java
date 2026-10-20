package com.aman.uptime.dto;

public record MonitorStatsResponse(
        Double uptime24h,
        Double uptime7d,
        Double uptime30d,
        Double avgResponseMs24h,
        long totalChecks24h,
        long failedChecks24h) {
}
