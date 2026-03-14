package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Details of a monitored route")
public record RouteResponse(
    @Schema(description = "Route identifier", example = "11111111-1111-1111-1111-111111111111")
    UUID id,
    @Schema(description = "IATA origin airport code", example = "WAW")
    String origin,
    @Schema(description = "IATA destination airport code", example = "LHR")
    String destination,
    @Schema(description = "Departure date", example = "2026-08-01")
    LocalDate departureDate,
    @Schema(description = "Whether monitoring for this route is active", example = "true")
    Boolean active,
    @Schema(description = "Creation timestamp", example = "2026-04-18T12:00:00Z")
    OffsetDateTime createdAt
) {
}
