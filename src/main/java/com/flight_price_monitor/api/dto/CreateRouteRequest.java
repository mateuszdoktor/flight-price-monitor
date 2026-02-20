package com.flight_price_monitor.api.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateRouteRequest(@NotBlank @Size(min = 3, max = 3) String origin,
                                 @NotBlank @Size(min = 3, max = 3) String destination,
                                 @NotNull @Future LocalDate departureDate) {
}
