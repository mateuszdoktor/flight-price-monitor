package com.flight_price_monitor.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PriceSnapshot(UUID id, UUID routeId, BigDecimal price, String currency, OffsetDateTime retrievedAt,
                            boolean isAnomaly) {
    public PriceSnapshot {
        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0)
            throw new IllegalArgumentException("Price can't be negative");
        if (currency == null || currency.isBlank() || currency.length() != 3)
            throw new IllegalArgumentException("Incorrect currency code");
        currency = currency.toUpperCase();
    }
}
