package com.aman.uptime.service;

/** Chhote pure helper methods (unit test karna easy hai, DB ki zarurat nahi). */
public final class UptimeCalculator {

    private UptimeCalculator() {
    }

    /**
     * Uptime percentage = (successful checks / total checks) * 100, 2 decimal tak.
     * Agar koi check hua hi nahi to null (UI me "-" dikhega).
     */
    public static Double percentage(long upChecks, long totalChecks) {
        if (totalChecks <= 0) {
            return null;
        }
        double pct = (upChecks * 100.0) / totalChecks;
        return Math.round(pct * 100.0) / 100.0;
    }

    /** Response ka status code expected code se match hona chahiye. */
    public static boolean isExpectedStatus(int actual, int expected) {
        return actual == expected;
    }

    /** 3725 seconds -> "1h 2m 5s" */
    public static String formatDuration(long totalSeconds) {
        long s = Math.max(totalSeconds, 0);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (h > 0) {
            return h + "h " + m + "m " + sec + "s";
        }
        if (m > 0) {
            return m + "m " + sec + "s";
        }
        return sec + "s";
    }
}
