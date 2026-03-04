package com.flight_price_monitor.persistence.repository;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshotEntity, UUID> {
    List<PriceSnapshotEntity> findByRouteIdOrderByRetrievedAtDesc(UUID routeId);

    Optional<PriceSnapshotEntity> findFirstByRouteIdOrderByRetrievedAtDesc(UUID routeId);

    List<PriceSnapshotEntity> findByRouteId(UUID routeId);

        @Query("""
            select ps
            from PriceSnapshotEntity ps
            where ps.route.id in :routeIds
            order by ps.route.id, ps.retrievedAt desc
            """)
        List<PriceSnapshotEntity> findAllByRouteIdInOrderByRouteIdAndRetrievedAtDesc(@Param("routeIds") Collection<UUID> routeIds);

        @Query("""
            select ps.price
            from PriceSnapshotEntity ps
            where ps.route.id = :routeId and ps.id <> :snapshotId
            """)
        List<BigDecimal> findHistoricalPricesByRouteIdExcludingSnapshotId(
            @Param("routeId") UUID routeId,
            @Param("snapshotId") UUID snapshotId
        );

    long countByRouteId(UUID routeId);
}
