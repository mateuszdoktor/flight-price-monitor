package com.flight_price_monitor.api.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record RouteStatisticsResponse(UUID routeId, String origin, String destination, LocalDate departureDate,
                                      BigDecimal mean, BigDecimal median, BigDecimal standardDeviation, BigDecimal min,
                                      BigDecimal max, int sampleCount, BigDecimal currentPrice, Double zScore) {
}
