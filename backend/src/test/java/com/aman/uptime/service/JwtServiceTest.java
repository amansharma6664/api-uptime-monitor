package com.aman.uptime.service;

import com.aman.uptime.security.JwtService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-long-enough-for-hs256-0123456789";

    @Test
    void tokenRoundTripReturnsEmail() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken("aman@example.com", "USER");
        assertEquals(Optional.of("aman@example.com"), jwt.extractEmail(token));
    }

    @Test
    void tamperedOrExpiredTokensAreRejected() {
        JwtService jwt = new JwtService(SECRET, 60_000);
        String token = jwt.generateToken("aman@example.com", "USER");
        assertTrue(jwt.extractEmail(token + "x").isEmpty());
        assertTrue(jwt.extractEmail("not-a-token").isEmpty());

        JwtService expired = new JwtService(SECRET, -1_000);
        assertTrue(expired.extractEmail(expired.generateToken("aman@example.com", "USER")).isEmpty());
    }
}
