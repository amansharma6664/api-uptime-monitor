package com.aman.uptime.service;

public record AlertMessage(Type type, String monitorName, String url, String email, String webhookUrl, String detail) {

    public enum Type {
        DOWN, RECOVERED
    }
}
