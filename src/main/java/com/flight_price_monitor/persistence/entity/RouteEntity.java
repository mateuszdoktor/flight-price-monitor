package com.flight_price_monitor.persistence.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "route", uniqueConstraints = {@UniqueConstraint(name = "route_origin_destination_departure_date_key",
        columnNames = {
                "origin",
                "destination",
                "departure_date"})})
public class RouteEntity {
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PriceSnapshotEntity> snapshots = new ArrayList<>();
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
    @CreatedDate
    @Column(name = "created_at", insertable = false, updatable = false, nullable = false)
    private OffsetDateTime createdAt;
}