package com.flight_price_monitor.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
        UUID routeId = snapshot.getRoute().getId();
        List<BigDecimal> historicalPrices = snapshotRepository.findHistoricalPricesByRouteIdExcludingSnapshotId(
                routeId,
                snapshot.getId()
        );
        if (historicalPrices.size() < anomalyProperties.minSamples()) {
            log.info(
                    "Not enough historical samples to evaluate anomaly, routeId={}, {} received, {} required",
                    routeId,
                    historicalPrices.size(),
                    anomalyProperties.minSamples()
            );
            return;
        }

        AnomalyEvaluation evaluation = evaluateAgainstHistory(historicalPrices, snapshot.getPrice());
        boolean isAnomaly = evaluation.isAnomaly();

        if (!Boolean.valueOf(isAnomaly).equals(snapshot.getIsAnomaly())) {
            snapshot.setIsAnomaly(isAnomaly);
            snapshotRepository.save(snapshot);
        }

        if (isAnomaly) {
            log.info(
                    "Detected anomaly for snapshot id={}, routeId={}, price={}, zScore={}, dropPercentage={}%",
                    snapshot.getId(),
                    routeId,
                    snapshot.getPrice(),
                    evaluation.statistics().zScore(),
                    evaluation.dropPercentage()
            );
            return;
        }

        log.info(
                "Did not detect anomaly for snapshot id={}, routeId={}, price={}, zScore={}, dropPercentage={}%",
                snapshot.getId(),
                routeId,
                snapshot.getPrice(),
                evaluation.statistics().zScore(),
                evaluation.dropPercentage()
        );
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
        if (routes.isEmpty()) {
            return List.of();
        }

        List<UUID> routeIds = routes.stream().map(RouteEntity::getId).toList();
        List<PriceSnapshotEntity> allSnapshots = snapshotRepository.findAllByRouteIdInOrderByRouteIdAndRetrievedAtDesc(routeIds);
        Map<UUID, List<PriceSnapshotEntity>> snapshotsByRoute = allSnapshots.stream()
            .collect(Collectors.groupingBy(snapshot -> snapshot.getRoute().getId(), LinkedHashMap::new, Collectors.toList()));

        return routes.stream().<DealResponse>mapMulti((route, consumer) -> {
            List<PriceSnapshotEntity> routeSnapshots = snapshotsByRoute.get(route.getId());
            if (routeSnapshots == null || routeSnapshots.isEmpty()) {
                return;
            }

            PriceSnapshotEntity latestSnapshot = routeSnapshots.getFirst();
            List<BigDecimal> historicalPrices = routeSnapshots.stream()
                .skip(1)
                .map(PriceSnapshotEntity::getPrice)
                .toList();
            if (historicalPrices.size() < anomalyProperties.minSamples()) {
                return;
            }

            AnomalyEvaluation evaluation = evaluateAgainstHistory(historicalPrices, latestSnapshot.getPrice());

            if (evaluation.isAnomaly()) {
                consumer.accept(DealResponse.builder()
                        .routeId(route.getId())
                        .origin(route.getOrigin())
                        .destination(route.getDestination())
                        .departureDate(route.getDepartureDate())
                .currentPrice(evaluation.statistics().currentPrice())
                .averagePrice(evaluation.statistics().mean())
                .dropPercentage(evaluation.dropPercentage())
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

    private AnomalyEvaluation evaluateAgainstHistory(List<BigDecimal> historicalPrices, BigDecimal currentPrice) {
        PriceStatistics statistics = AnomalyDetector.buildStatistics(historicalPrices, currentPrice);
        BigDecimal dropPercentage = calculateDropPercentage(statistics.mean(), currentPrice);

        boolean anomalyByZScore = AnomalyDetector.isAnomalyByZScore(
                statistics.zScore(),
                anomalyProperties.zScoreThreshold()
        );
        boolean anomalyByPercentage = AnomalyDetector.isAnomalyByPercentage(
                currentPrice,
                statistics.mean(),
                anomalyProperties.percentageThreshold()
        );

        return new AnomalyEvaluation(statistics, dropPercentage, anomalyByZScore || anomalyByPercentage);
    }

    private BigDecimal calculateDropPercentage(BigDecimal mean, BigDecimal currentPrice) {
        return mean
                .subtract(currentPrice)
                .divide(mean, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    private record AnomalyEvaluation(PriceStatistics statistics, BigDecimal dropPercentage, boolean isAnomaly) {
    }
}
