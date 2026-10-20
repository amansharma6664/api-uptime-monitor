package com.aman.uptime.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UptimeCalculatorTest {

    @Test
    void percentageIsNullWhenNoChecks() {
        assertNull(UptimeCalculator.percentage(0, 0));
    }

    @Test
    void percentageIsRoundedToTwoDecimals() {
        assertEquals(100.0, UptimeCalculator.percentage(10, 10));
        assertEquals(99.0, UptimeCalculator.percentage(99, 100));
        assertEquals(33.33, UptimeCalculator.percentage(1, 3));
        assertEquals(0.0, UptimeCalculator.percentage(0, 5));
    }

    @Test
    void statusMustMatchExactly() {
        assertTrue(UptimeCalculator.isExpectedStatus(200, 200));
        assertFalse(UptimeCalculator.isExpectedStatus(404, 200));
    }

    @Test
    void durationIsFormattedForHumans() {
        assertEquals("5s", UptimeCalculator.formatDuration(5));
        assertEquals("2m 3s", UptimeCalculator.formatDuration(123));
        assertEquals("1h 2m 5s", UptimeCalculator.formatDuration(3725));
        assertEquals("0s", UptimeCalculator.formatDuration(-4));
    }
}
