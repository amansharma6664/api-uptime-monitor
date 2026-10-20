package com.aman.uptime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Monitor DOWN hone par / wapas UP aane par alert bhejta hai:
 *  1) hamesha console log me
 *  2) email (agar app.alert.email.enabled=true aur SMTP configured ho)
 *  3) webhook (Slack / Discord / koi bhi URL) agar monitor me webhookUrl diya ho
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final ObjectMapper objectMapper;
    private final boolean emailEnabled;
    private final String fromAddress;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public AlertService(ObjectProvider<JavaMailSender> mailSenderProvider,
                        ObjectMapper objectMapper,
                        @Value("${app.alert.email.enabled:false}") boolean emailEnabled,
                        @Value("${app.alert.email.from:alerts@uptime-monitor.local}") String fromAddress) {
        this.mailSenderProvider = mailSenderProvider;
        this.objectMapper = objectMapper;
        this.emailEnabled = emailEnabled;
        this.fromAddress = fromAddress;
    }

    public void send(AlertMessage alert) {
        String subject = alert.type() == AlertMessage.Type.DOWN
                ? "[DOWN] " + alert.monitorName() + " is down"
                : "[RECOVERED] " + alert.monitorName() + " is back up";
        String body = "Monitor: " + alert.monitorName() + "\n"
                + "URL: " + alert.url() + "\n"
                + "Details: " + alert.detail();

        log.warn("ALERT {} | {} | {} | {}", alert.type(), alert.monitorName(), alert.url(), alert.detail());
        sendEmail(alert, subject, body);
        sendWebhook(alert, subject + "\n" + body);
    }

    private void sendEmail(AlertMessage alert, String subject, String body) {
        if (!emailEnabled || alert.email() == null || alert.email().isBlank()) {
            return;
        }
        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(alert.email());
            message.setSubject(subject);
            message.setText(body);
            sender.send(message);
            log.info("Alert email sent to {}", alert.email());
        } catch (Exception ex) {
            log.warn("Could not send alert email: {}", ex.getMessage());
        }
    }

    private void sendWebhook(AlertMessage alert, String text) {
        if (alert.webhookUrl() == null || alert.webhookUrl().isBlank()) {
            return;
        }
        try {
            // "text" Slack ke liye, "content" Discord ke liye
            String json = objectMapper.writeValueAsString(Map.of("text", text, "content", text));
            HttpRequest request = HttpRequest.newBuilder(URI.create(alert.webhookUrl()))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            log.info("Webhook alert sent, HTTP {}", response.statusCode());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Exception ex) {
            log.warn("Could not send webhook alert: {}", ex.getMessage());
        }
    }
}
