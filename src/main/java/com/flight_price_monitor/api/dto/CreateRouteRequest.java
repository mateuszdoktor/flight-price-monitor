package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Request payload used to create a monitored route")
public record CreateRouteRequest(
    @Schema(description = "IATA origin airport code", example = "WAW", minLength = 3, maxLength = 3)
    @NotBlank @Size(min = 3, max = 3) String origin,
    @Schema(description = "IATA destination airport code", example = "LHR", minLength = 3, maxLength = 3)
    @NotBlank @Size(min = 3, max = 3) String destination,
    @Schema(description = "Departure date in ISO-8601 format", example = "2026-08-01")
    @NotNull @Future LocalDate departureDate
) {
}
