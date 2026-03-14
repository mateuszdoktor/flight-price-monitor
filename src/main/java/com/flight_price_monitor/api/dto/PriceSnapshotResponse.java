package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Single historical price snapshot for a route")
public record PriceSnapshotResponse(
    @Schema(description = "Snapshot identifier", example = "11111111-1111-1111-1111-111111111111")
    UUID id,
    @Schema(description = "Observed flight price", example = "199.99")
    BigDecimal price,
    @Schema(description = "ISO currency code", example = "EUR")
    String currency,
    @Schema(description = "Snapshot retrieval timestamp", example = "2026-04-18T11:45:00Z")
    OffsetDateTime retrievedAt,
    @Schema(description = "Whether this snapshot was flagged as anomaly", example = "false")
    boolean anomaly
) {
}
