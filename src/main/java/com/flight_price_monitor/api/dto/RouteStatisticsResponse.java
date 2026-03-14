package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Builder
@Schema(description = "Aggregated statistics calculated from route price history")
public record RouteStatisticsResponse(
    @Schema(description = "Route identifier", example = "11111111-1111-1111-1111-111111111111")
    UUID routeId,
    @Schema(description = "IATA origin airport code", example = "WAW")
    String origin,
    @Schema(description = "IATA destination airport code", example = "LHR")
    String destination,
    @Schema(description = "Departure date", example = "2026-08-01")
    LocalDate departureDate,
    @Schema(description = "Mean price", example = "220.15")
    BigDecimal mean,
    @Schema(description = "Median price", example = "210.00")
    BigDecimal median,
    @Schema(description = "Standard deviation", example = "34.5123")
    BigDecimal standardDeviation,
    @Schema(description = "Minimum observed price", example = "149.99")
    BigDecimal min,
    @Schema(description = "Maximum observed price", example = "299.99")
    BigDecimal max,
    @Schema(description = "Number of samples", example = "12")
    int sampleCount,
    @Schema(description = "Current/latest observed price", example = "189.99")
    BigDecimal currentPrice,
    @Schema(description = "Z-score for current price", example = "-1.78")
    Double zScore
) {
}
