package com.flight_price_monitor.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.flight_price_monitor.config.AnomalyProperties;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.mapper.PriceSnapshotMapper;
import com.flight_price_monitor.persistence.repository.PriceSnapshotRepository;
import com.flight_price_monitor.persistence.repository.RouteRepository;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    RouteRepository routeRepository;

    @Mock
    PriceSnapshotRepository snapshotRepository;

    @Mock
    AnomalyProperties anomalyProperties;

    @Mock
    @SuppressWarnings("unused")
    PriceSnapshotMapper priceSnapshotMapper;

    @InjectMocks
    AnomalyDetectionService anomalyDetectionService;

    // Helpers

    private RouteEntity buildRoute(UUID id) {
        RouteEntity route = new RouteEntity();
        route.setId(id);
        route.setOrigin("KRK");
        route.setDestination("BAH");
        route.setDepartureDate(LocalDate.now().plusMonths(3));
        route.setActive(true);
        route.setCreatedAt(OffsetDateTime.now());
        return route;
    }

    private PriceSnapshotEntity buildSnapshot(RouteEntity route, BigDecimal price) {
        return PriceSnapshotEntity.builder()
                .id(UUID.randomUUID())
                .route(route)
                .price(price)
                .currency("EUR")
                .isAnomaly(false)
                .build();
    }

    private List<PriceSnapshotEntity> fiveHistoricalSnapshots(RouteEntity route) {
        return List.of(
                buildSnapshot(route, new BigDecimal("100.00")),
                buildSnapshot(route, new BigDecimal("150.00")),
                buildSnapshot(route, new BigDecimal("200.00")),
                buildSnapshot(route, new BigDecimal("250.00")),
                buildSnapshot(route, new BigDecimal("300.00"))
        );
    }

    @Test
    void evaluateAnomaly_withEnoughData_detectsAnomaly() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity currentSnapshot = buildSnapshot(route, new BigDecimal("50.00"));

        when(anomalyProperties.minSamples()).thenReturn(5);
        when(anomalyProperties.zScoreThreshold()).thenReturn(2.0);
        when(anomalyProperties.percentageThreshold()).thenReturn(0.7);
        when(snapshotRepository.findHistoricalPricesByRouteIdExcludingSnapshotId(routeId, currentSnapshot.getId()))
                .thenReturn(fiveHistoricalSnapshots(route).stream().map(PriceSnapshotEntity::getPrice).toList());

        anomalyDetectionService.evaluateAnomaly(currentSnapshot);

        assertTrue(currentSnapshot.getIsAnomaly(), "snapshot with very low price should be marked as anomaly");
        verify(snapshotRepository, times(1)).save(currentSnapshot);
    }

    @Test
    void evaluateAnomaly_withEnoughData_noAnomaly() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity currentSnapshot = buildSnapshot(route, new BigDecimal("180.00"));

        when(anomalyProperties.minSamples()).thenReturn(5);
        when(anomalyProperties.zScoreThreshold()).thenReturn(2.0);
        when(anomalyProperties.percentageThreshold()).thenReturn(0.7);
        when(snapshotRepository.findHistoricalPricesByRouteIdExcludingSnapshotId(routeId, currentSnapshot.getId()))
                .thenReturn(fiveHistoricalSnapshots(route).stream().map(PriceSnapshotEntity::getPrice).toList());

        anomalyDetectionService.evaluateAnomaly(currentSnapshot);

        assertFalse(currentSnapshot.getIsAnomaly(), "snapshot with normal price should NOT be flagged");
        verify(snapshotRepository, never()).save(any());
    }

    @Test
    void evaluateAnomaly_withInsufficientData_skipsEvaluation() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity currentSnapshot = buildSnapshot(route, new BigDecimal("50.00"));

        List<BigDecimal> tooFewHistoricalPrices = List.of(
                new BigDecimal("200.00"),
                new BigDecimal("210.00"),
                new BigDecimal("190.00")
        );

        when(anomalyProperties.minSamples()).thenReturn(5);
        when(snapshotRepository.findHistoricalPricesByRouteIdExcludingSnapshotId(routeId, currentSnapshot.getId()))
                .thenReturn(tooFewHistoricalPrices);

        anomalyDetectionService.evaluateAnomaly(currentSnapshot);

        assertFalse(currentSnapshot.getIsAnomaly(), "anomaly detection should be skipped with insufficient data");
        verify(snapshotRepository, never()).save(any());
        verify(anomalyProperties, never()).zScoreThreshold();
        verify(anomalyProperties, never()).percentageThreshold();
    }

    @Test
    void evaluateAnomaly_withPercentageDrop_detectsAnomaly() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity currentSnapshot = buildSnapshot(route, new BigDecimal("70.00"));

        when(anomalyProperties.minSamples()).thenReturn(5);
        when(anomalyProperties.zScoreThreshold()).thenReturn(10.0);
        when(anomalyProperties.percentageThreshold()).thenReturn(0.7);
        when(snapshotRepository.findHistoricalPricesByRouteIdExcludingSnapshotId(routeId, currentSnapshot.getId()))
                .thenReturn(fiveHistoricalSnapshots(route).stream().map(PriceSnapshotEntity::getPrice).toList());

        anomalyDetectionService.evaluateAnomaly(currentSnapshot);

        assertTrue(currentSnapshot.getIsAnomaly(), "percentage-based drop should mark snapshot as anomaly");
        verify(snapshotRepository, times(1)).save(currentSnapshot);
    }

    @Test
    void getStatistics_withSnapshots_returnsCorrectStats() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        List<PriceSnapshotEntity> snapshots = fiveHistoricalSnapshots(route);
        PriceSnapshotEntity latestSnapshot = snapshots.get(snapshots.size() - 1);

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(snapshotRepository.findByRouteId(routeId)).thenReturn(snapshots);
        when(snapshotRepository.findFirstByRouteIdOrderByRetrievedAtDesc(routeId))
                .thenReturn(Optional.of(latestSnapshot));

        var stats = anomalyDetectionService.getStatistics(routeId);

        assertEquals(routeId, stats.routeId());
        assertEquals(new java.math.BigDecimal("200.00"), stats.mean());
        assertEquals(new java.math.BigDecimal("300.00"), stats.currentPrice());
        assertEquals(5, stats.sampleCount());
    }

    @Test
    void getStatistics_noSnapshots_throwsInsufficientDataException() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        when(routeRepository.findById(routeId)).thenReturn(Optional.of(route));
        when(snapshotRepository.findByRouteId(routeId)).thenReturn(List.of());
        when(anomalyProperties.minSamples()).thenReturn(5);

        var exception = assertThrows(
                com.flight_price_monitor.common.exception.InsufficientDataException.class,
                () -> anomalyDetectionService.getStatistics(routeId)
        );
        assertNotNull(exception);
    }

    @Test
    void getCurrentDeals_filtersAnomalies_returnsDeals() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity latestSnapshot = buildSnapshot(route, new BigDecimal("50.00"));
        List<PriceSnapshotEntity> allSnapshots = List.of(
                latestSnapshot,
                buildSnapshot(route, new BigDecimal("100.00")),
                buildSnapshot(route, new BigDecimal("150.00")),
                buildSnapshot(route, new BigDecimal("200.00")),
                buildSnapshot(route, new BigDecimal("250.00")),
                buildSnapshot(route, new BigDecimal("300.00"))
        );

        when(routeRepository.findAllByActiveTrue()).thenReturn(List.of(route));
        when(snapshotRepository.findAllByRouteIdInOrderByRouteIdAndRetrievedAtDesc(any())).thenReturn(allSnapshots);
        when(anomalyProperties.minSamples()).thenReturn(5);
        when(anomalyProperties.zScoreThreshold()).thenReturn(2.0);
        when(anomalyProperties.percentageThreshold()).thenReturn(0.7);

        var deals = anomalyDetectionService.getCurrentDeals();

        assertFalse(deals.isEmpty(), "anomalous route should appear in deals");
        assertEquals(routeId, deals.get(0).routeId());
        assertTrue(deals.get(0).dropPercentage().compareTo(BigDecimal.ZERO) > 0,
                "drop percentage should be positive");
    }

    @Test
    void getCurrentDeals_noDeals_returnsEmptyList() {
        UUID routeId = UUID.randomUUID();
        RouteEntity route = buildRoute(routeId);

        PriceSnapshotEntity latestSnapshot = buildSnapshot(route, new BigDecimal("195.00"));
        List<PriceSnapshotEntity> allSnapshots = List.of(
                latestSnapshot,
                buildSnapshot(route, new BigDecimal("100.00")),
                buildSnapshot(route, new BigDecimal("150.00")),
                buildSnapshot(route, new BigDecimal("200.00")),
                buildSnapshot(route, new BigDecimal("250.00")),
                buildSnapshot(route, new BigDecimal("300.00"))
        );

        when(routeRepository.findAllByActiveTrue()).thenReturn(List.of(route));
        when(snapshotRepository.findAllByRouteIdInOrderByRouteIdAndRetrievedAtDesc(any())).thenReturn(allSnapshots);
        when(anomalyProperties.minSamples()).thenReturn(5);
        when(anomalyProperties.zScoreThreshold()).thenReturn(2.0);
        when(anomalyProperties.percentageThreshold()).thenReturn(0.7);

        var deals = anomalyDetectionService.getCurrentDeals();

        assertTrue(deals.isEmpty(), "no anomalies should mean empty deals list");
    }
}
