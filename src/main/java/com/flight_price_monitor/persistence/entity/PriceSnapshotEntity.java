package com.flight_price_monitor.persistence.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "price_snapshot", indexes = {
        @Index(name = "idx_price_snapshot_is_anomaly",
                columnList = "route_id"),
        @Index(name = "idx_price_snapshot_route_id",
                columnList = "route_id"),
        @Index(name = "idx_price_snapshot_retrieved_at",
                columnList = "retrieved_at")})
@NoArgsConstructor
@AllArgsConstructor
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
    @CreatedDate
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