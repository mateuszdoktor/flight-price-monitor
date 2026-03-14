package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
@Schema(description = "Detected flight deal for a monitored route")
public record DealResponse(
    @Schema(description = "Route identifier", example = "11111111-1111-1111-1111-111111111111")
    UUID routeId,
    @Schema(description = "IATA origin airport code", example = "WAW")
    String origin,
    @Schema(description = "IATA destination airport code", example = "LHR")
    String destination,
    @Schema(description = "Departure date", example = "2026-08-01")
    LocalDate departureDate,
    @Schema(description = "Current observed price", example = "129.99")
    BigDecimal currentPrice,
    @Schema(description = "Historical average price", example = "219.99")
    BigDecimal averagePrice,
    @Schema(description = "Price drop percentage against historical mean", example = "40.91")
    BigDecimal dropPercentage,
    @Schema(description = "ISO currency code", example = "EUR")
    String currency,
    @Schema(description = "Timestamp of current price retrieval", example = "2026-04-18T11:45:00Z")
    OffsetDateTime retrievedAt
) {
}
