package com.aman.uptime.dto;

public record AdminOverviewResponse(long totalUsers, long totalMonitors, long totalChecks, long totalIncidents) {
}
