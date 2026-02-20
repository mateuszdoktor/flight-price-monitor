package com.flight_price_monitor.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DealResponse(UUID routeId, String origin, String destination, LocalDate departureDate,
                           BigDecimal currentPrice, BigDecimal averagePrice, BigDecimal dropPercentage,
                           String currency, OffsetDateTime retrievedAt) {
}
