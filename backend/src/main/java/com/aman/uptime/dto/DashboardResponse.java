package com.aman.uptime.dto;

public record DashboardResponse(
        long totalMonitors,
        long up,
        long down,
        long paused,
        long pending,
        Double averageUptime24h,
        long openIncidents) {
}
