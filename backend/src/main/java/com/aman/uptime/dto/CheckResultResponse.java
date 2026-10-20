package com.aman.uptime.dto;

import com.aman.uptime.entity.CheckStatus;

import java.time.Instant;

public record CheckResultResponse(
        Long id,
        CheckStatus status,
        Integer statusCode,
        long responseTimeMs,
        String errorMessage,
        Instant checkedAt) {
}
