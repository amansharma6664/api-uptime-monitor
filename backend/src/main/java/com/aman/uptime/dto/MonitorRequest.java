package com.aman.uptime.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MonitorRequest(
        @NotBlank(message = "Name is required") String name,
        @NotBlank(message = "URL is required")
        @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://") String url,
        @Pattern(regexp = "^(GET|HEAD)$", message = "Method must be GET or HEAD") String httpMethod,
        @Min(value = 10, message = "Interval must be at least 10 seconds")
        @Max(value = 3600, message = "Interval can be at most 3600 seconds") Integer intervalSeconds,
        @Min(value = 1, message = "Timeout must be at least 1 second")
        @Max(value = 60, message = "Timeout can be at most 60 seconds") Integer timeoutSeconds,
        @Min(value = 100, message = "Expected status must be a valid HTTP code")
        @Max(value = 599, message = "Expected status must be a valid HTTP code") Integer expectedStatus,
        String webhookUrl,
        String alertEmail) {
}
