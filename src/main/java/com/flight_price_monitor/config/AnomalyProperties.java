package com.flight_price_monitor.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "anomaly")
public record AnomalyProperties(int minSamples, double zScoreThreshold, double percentageThreshold) {
}
