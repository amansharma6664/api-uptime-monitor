package com.aman.uptime.service;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

/** Asli HTTP request bhejta hai aur response time naapta hai (java.net.http.HttpClient). */
@Service
public class HttpCheckService {

    private final HttpClient client = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public CheckOutcome check(String url, String method, int timeoutSeconds, int expectedStatus) {
        long start = System.nanoTime();
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .header("User-Agent", "UptimeMonitor/1.0")
                    .method(method, HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            int code = response.statusCode();
            if (UptimeCalculator.isExpectedStatus(code, expectedStatus)) {
                return new CheckOutcome(true, code, elapsedMs, null);
            }
            return new CheckOutcome(false, code, elapsedMs,
                    "Expected HTTP " + expectedStatus + " but got HTTP " + code);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return new CheckOutcome(false, null, elapsedSince(start), "Check was interrupted");
        } catch (Exception ex) {
            return new CheckOutcome(false, null, elapsedSince(start), describe(ex));
        }
    }

    private long elapsedSince(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String describe(Exception ex) {
        String msg;
        if (ex instanceof HttpTimeoutException) {
            msg = "Request timed out";
        } else if (ex.getMessage() != null) {
            msg = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        } else {
            msg = ex.getClass().getSimpleName();
        }
        return msg.length() > 450 ? msg.substring(0, 450) : msg;
    }
}
