package com.flight_price_monitor.persistence.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "route", uniqueConstraints = {@UniqueConstraint(name = "route_origin_destination_departure_date_key",
        columnNames = {
                "origin",
                "destination",
                "departure_date"})})
public class RouteEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Size(min = 3, max = 3)
    @NotBlank
    @Column(name = "origin", nullable = false, length = 3)
    private String origin;

    @Size(min = 3, max = 3)
    @NotBlank
    @Column(name = "destination", nullable = false, length = 3)
    private String destination;

    @NotNull
    @Column(name = "departure_date", nullable = false)
    private LocalDate departureDate;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "active", nullable = false)
    private Boolean active;

    @NotNull
    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PriceSnapshotEntity> snapshots = new ArrayList<>();
}