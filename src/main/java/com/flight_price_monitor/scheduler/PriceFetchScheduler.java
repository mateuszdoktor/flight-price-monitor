package com.flight_price_monitor.scheduler;

import com.flight_price_monitor.application.PriceMonitoringService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PriceFetchScheduler {
    private final PriceMonitoringService priceMonitoringService;

    public PriceFetchScheduler(PriceMonitoringService priceMonitoringService) {
        this.priceMonitoringService = priceMonitoringService;
    }

    @Scheduled(fixedRateString = "${scheduler.price-fetch.interval-ms:21600000}")
    public void fetchPrices() {
        try {
            log.info("Starting scheduled price fetch...");
            priceMonitoringService.fetchPricesForAllActiveRoutes();
            log.info("Scheduled price fetch completed");
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
