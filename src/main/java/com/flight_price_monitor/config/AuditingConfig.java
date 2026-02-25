package com.flight_price_monitor.config;

import java.time.OffsetDateTime;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

@Configuration
public class AuditingConfig {

    @Bean
    public DateTimeProvider offsetDateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
}
