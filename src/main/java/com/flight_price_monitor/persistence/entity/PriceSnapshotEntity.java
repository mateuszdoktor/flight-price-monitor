package com.flight_price_monitor.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "price_snapshot", indexes = {
        @Index(name = "idx_price_snapshot_anomalies",
                columnList = "route_id"),
        @Index(name = "idx_price_snapshot_route_id",
                columnList = "route_id"),
        @Index(name = "idx_price_snapshot_retrieved_at",
                columnList = "retrieved_at")})
public class PriceSnapshotEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @NotNull
    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Size(min = 3, max = 3)
    @NotBlank
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "retrieved_at", insertable = false, updatable = false, nullable = false)
    private OffsetDateTime retrievedAt;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_anomaly", nullable = false)
    private Boolean isAnomaly;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private RouteEntity route;
}