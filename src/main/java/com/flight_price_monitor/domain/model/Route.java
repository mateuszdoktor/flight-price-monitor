package com.flight_price_monitor.domain.model;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

public record Route(UUID id, String origin, String destination, LocalDate departureDate, boolean active,
                    OffsetDateTime createdAt) {
    public Route {
        if (origin == null || origin.isBlank())
            throw new IllegalArgumentException("Origin can't be null nor blank");
        if (destination == null || destination.isBlank())
            throw new IllegalArgumentException("Destination can't be null nor blank");
        Objects.requireNonNull(departureDate, "Departure can't be null");
    }
}
