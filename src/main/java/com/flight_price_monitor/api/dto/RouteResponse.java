package com.flight_price_monitor.api.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record RouteResponse(UUID id, String origin, String destination, LocalDate departureDate, Boolean active,
                            OffsetDateTime createdAt) {
}
