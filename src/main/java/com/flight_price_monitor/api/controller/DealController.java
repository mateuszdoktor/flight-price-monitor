package com.flight_price_monitor.api.controller;

import com.flight_price_monitor.api.dto.DealResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/deals")
public class DealController {
    private final AnomalyDetectionService anomalyDetectionService;

    public DealController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping
    public ResponseEntity<List<DealResponse>> getDeals() {
        var deals = anomalyDetectionService.getCurrentDeals();
        return ResponseEntity.ok(deals);
    }
}
