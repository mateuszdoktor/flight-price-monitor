package com.flight_price_monitor.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flight_price_monitor.api.dto.DealResponse;
import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.api.dto.RouteStatisticsResponse;
import com.flight_price_monitor.common.exception.InsufficientDataException;
import com.flight_price_monitor.common.exception.RouteNotFoundException;
import com.flight_price_monitor.config.AnomalyProperties;
import com.flight_price_monitor.domain.model.AnomalyDetector;
import com.flight_price_monitor.domain.model.PriceStatistics;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.mapper.PriceSnapshotMapper;
import com.flight_price_monitor.persistence.repository.PriceSnapshotRepository;
import com.flight_price_monitor.persistence.repository.RouteRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AnomalyDetectionService {

    private final RouteRepository routeRepository;
    private final PriceSnapshotRepository snapshotRepository;
    private final AnomalyProperties anomalyProperties;
    private final PriceSnapshotMapper priceSnapshotMapper;

    public AnomalyDetectionService(RouteRepository routeRepository, PriceSnapshotRepository snapshotRepository,
                                   AnomalyProperties anomalyProperties, PriceSnapshotMapper priceSnapshotMapper) {
        this.routeRepository = routeRepository;
        this.snapshotRepository = snapshotRepository;
        this.anomalyProperties = anomalyProperties;
        this.priceSnapshotMapper = priceSnapshotMapper;
    }

    @Transactional
    public void evaluateAnomaly(PriceSnapshotEntity snapshot) {
        List<PriceSnapshotEntity> snapshots = snapshotRepository.findByRouteId(snapshot.getRoute().getId());
        if (snapshots.size() < anomalyProperties.minSamples()) {
            log.info("Not enough samples to evaluate anomaly, {} received, {} required", snapshots.size(), anomalyProperties.minSamples());
            return;
        }

        List<BigDecimal> prices = snapshots.stream().map(PriceSnapshotEntity::getPrice).toList();
        PriceStatistics statistics = AnomalyDetector.buildStatistics(prices, snapshot.getPrice());

        if (AnomalyDetector.isAnomalyByZScore(statistics.zScore(), anomalyProperties.zScoreThreshold())) {
            snapshot.setIsAnomaly(true);
            snapshotRepository.save(snapshot);
            log.info("Detected anomaly for price snapshot, id={}, price={}", snapshot.getId(), snapshot.getPrice());
            return;
        }

        log.info("Did not detect anomaly for price snapshot, id={}, price={}", snapshot.getId(), snapshot.getPrice());
    }

    @Transactional(readOnly = true)
    public RouteStatisticsResponse getStatistics(UUID routeId) {
        RouteEntity route = routeRepository.findById(routeId).orElseThrow(() -> new RouteNotFoundException(routeId));
        List<PriceSnapshotEntity> snapshots = snapshotRepository.findByRouteId(routeId);
        if (snapshots.isEmpty())
            throw new InsufficientDataException(routeId, anomalyProperties.minSamples(), 0);

        List<BigDecimal> prices = snapshots.stream().map(PriceSnapshotEntity::getPrice).toList();
        PriceSnapshotEntity latestSnapshot = snapshotRepository.findFirstByRouteIdOrderByRetrievedAtDesc(routeId)
                .orElseThrow(() -> new InsufficientDataException(routeId, anomalyProperties.minSamples(), 0));
        PriceStatistics statistics = AnomalyDetector.buildStatistics(prices, latestSnapshot.getPrice());

        return RouteStatisticsResponse.builder()
                .routeId(routeId)
                .origin(route.getOrigin())
                .destination(route.getDestination())
                .departureDate(route.getDepartureDate())
                .mean(statistics.mean())
                .median(statistics.median())
                .standardDeviation(statistics.standardDeviation())
                .min(statistics.min())
                .max(statistics.max())
                .sampleCount(prices.size())
                .currentPrice(statistics.currentPrice())
                .zScore(statistics.zScore())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DealResponse> getCurrentDeals() {
        List<RouteEntity> routes = routeRepository.findAllByActiveTrue();
        return routes.stream().<DealResponse>mapMulti((route, consumer) -> {
            List<PriceSnapshotEntity> snapshots = snapshotRepository.findByRouteId(route.getId());
            if (snapshots.size() < anomalyProperties.minSamples()) {
                return;
            }

            PriceSnapshotEntity latestSnapshot = snapshotRepository.findFirstByRouteIdOrderByRetrievedAtDesc(route.getId())
                    .orElse(null);
            if (latestSnapshot == null) {
                return;
            }

            BigDecimal currentPrice = latestSnapshot.getPrice();
            List<BigDecimal> prices = snapshots.stream().map(PriceSnapshotEntity::getPrice).toList();
            PriceStatistics statistics = AnomalyDetector.buildStatistics(prices, currentPrice);

            BigDecimal dropPercentage = statistics.mean()
                    .subtract(currentPrice)
                    .divide(statistics.mean(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            boolean anomalyByZScore = AnomalyDetector.isAnomalyByZScore(
                    statistics.zScore(),
                    anomalyProperties.zScoreThreshold()
            );
            boolean anomalyByPercentage = dropPercentage.compareTo(
                    BigDecimal.valueOf((1 - anomalyProperties.percentageThreshold()) * 100)
            ) > 0;

            if (anomalyByZScore || anomalyByPercentage) {
                consumer.accept(DealResponse.builder()
                        .routeId(route.getId())
                        .origin(route.getOrigin())
                        .destination(route.getDestination())
                        .departureDate(route.getDepartureDate())
                        .currentPrice(statistics.currentPrice())
                        .averagePrice(statistics.mean())
                        .dropPercentage(dropPercentage)
                        .currency(latestSnapshot.getCurrency())
                        .retrievedAt(latestSnapshot.getRetrievedAt())
                        .build());
            }
        }).sorted(Comparator.comparing(DealResponse::dropPercentage).reversed()).toList();
    }

    @Transactional(readOnly = true)
    public List<PriceSnapshotResponse> getSnapshotsForRoute(UUID routeId) {
        routeRepository.findById(routeId).orElseThrow(() -> new RouteNotFoundException(routeId));
        List<PriceSnapshotEntity> snapshots = snapshotRepository.findByRouteIdOrderByRetrievedAtDesc(routeId);
        return snapshots.stream().map(priceSnapshotMapper::toResponse).toList();
    }
}
