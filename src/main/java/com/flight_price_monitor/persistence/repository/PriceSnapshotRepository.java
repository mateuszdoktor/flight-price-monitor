package com.flight_price_monitor.persistence.repository;

import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshotEntity, UUID> {
    List<PriceSnapshotEntity> findByRouteIdOrderByRetrievedAtDesc(UUID routeId);

    Optional<PriceSnapshotEntity> findFirstByRouteIdOrderByRetrievedAtDesc(UUID routeId);

    List<PriceSnapshotEntity> findByRouteId(UUID routeId);

    long countByRouteId(UUID routeId);
}
