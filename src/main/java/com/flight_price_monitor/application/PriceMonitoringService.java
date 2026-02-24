package com.flight_price_monitor.application;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.flight_price_monitor.infrastructure.amadeus.AmadeusClient;
import com.flight_price_monitor.infrastructure.amadeus.FlightPrice;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.repository.PriceSnapshotRepository;
import com.flight_price_monitor.persistence.repository.RouteRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PriceMonitoringService {
    private final RouteRepository routeRepository;
    private final PriceSnapshotRepository snapshotRepository;
    private final AmadeusClient amadeusClient;
    private final AnomalyDetectionService anomalyDetectionService;

    public PriceMonitoringService(RouteRepository routeRepository,
                                 PriceSnapshotRepository snapshotRepository,
                                 AmadeusClient amadeusClient,
                                 AnomalyDetectionService anomalyDetectionService) {
        this.routeRepository = routeRepository;
        this.snapshotRepository = snapshotRepository;
        this.amadeusClient = amadeusClient;
        this.anomalyDetectionService = anomalyDetectionService;
    }


    @Transactional
    public void fetchPricesForAllActiveRoutes() {
        AtomicInteger successfulCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        var activeRoutes = routeRepository.findAllByActiveTrue();

        activeRoutes.forEach(route -> {
            try {
                fetchAndSavePrice(route);
                successfulCount.incrementAndGet();
            } catch (Exception e) {
                log.warn("Route error {}: {}", route.getId(), e.getMessage());
                errorCount.incrementAndGet();
            }
        });
        log.info(
                "Finished fetching and saving flight prices, {} successful, {} failed",
                successfulCount.get(),
                errorCount.get()
        );
    }

    private void fetchAndSavePrice(RouteEntity route) {
        FlightPrice flightPrice = amadeusClient.getLowestPrice(route.getOrigin(), route.getDestination(), route.getDepartureDate());
        PriceSnapshotEntity snapshot = PriceSnapshotEntity.builder()
                .price(flightPrice.price())
                .currency(flightPrice.currency())
                .route(route)
                .isAnomaly(false)
                .build();
        PriceSnapshotEntity savedSnapshot = snapshotRepository.save(snapshot);
        anomalyDetectionService.evaluateAnomaly(savedSnapshot);
    }
}
