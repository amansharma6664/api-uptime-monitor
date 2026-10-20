package com.aman.uptime.service;

/** Ek HTTP check ka result (DB me save hone se pehle). */
public record CheckOutcome(boolean up, Integer statusCode, long responseTimeMs, String error) {
}
