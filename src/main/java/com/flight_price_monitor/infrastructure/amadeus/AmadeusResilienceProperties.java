package com.flight_price_monitor.infrastructure.amadeus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amadeus.resilience")
public record AmadeusResilienceProperties(
        int retryMaxAttempts,
        long retryWaitDurationMs,
        int circuitBreakerSlidingWindowSize,
        int circuitBreakerMinimumNumberOfCalls,
        double circuitBreakerFailureRateThreshold,
        long circuitBreakerWaitDurationMs,
        long timeoutMs,
        long fallbackMaxAgeMinutes
) {
}
