package com.aman.uptime.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class AppConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Health checks parallel me chalane ke liye thread pool.
     * Scheduler sirf due monitors dhoondta hai, asli HTTP call yahan ke threads karte hain.
     */
    @Bean(destroyMethod = "shutdown")
    public ExecutorService checkExecutor(@Value("${app.monitor.worker-threads:10}") int threads) {
        AtomicInteger counter = new AtomicInteger(1);
        ThreadFactory factory = runnable -> {
            Thread t = new Thread(runnable, "monitor-check-" + counter.getAndIncrement());
            t.setDaemon(true);
            return t;
        };
        return Executors.newFixedThreadPool(threads, factory);
    }
}
