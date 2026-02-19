package com.flight_price_monitor.domain.model;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PriceStatistics(BigDecimal mean, BigDecimal median, BigDecimal standardDeviation, BigDecimal min,
                              BigDecimal max, int sampleCount, BigDecimal currentPrice, Double zScore) {
}
