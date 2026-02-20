package com.flight_price_monitor.api.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PriceSnapshotResponse(UUID id, BigDecimal price, String currency, OffsetDateTime retrievedAt,
                                    boolean anomaly) {
}
