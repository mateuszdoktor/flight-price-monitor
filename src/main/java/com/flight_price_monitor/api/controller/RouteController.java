package com.flight_price_monitor.api.controller;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.api.dto.RouteStatisticsResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import com.flight_price_monitor.application.RouteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/routes")
public class RouteController {
    private final RouteService routeService;
    private final AnomalyDetectionService anomalyDetectionService;

    public RouteController(RouteService routeService, AnomalyDetectionService anomalyDetectionService) {
        this.routeService = routeService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @PostMapping
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        var route = routeService.createRoute(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(route.id()).toUri();
        return ResponseEntity.created(location).body(route);
    }

    @GetMapping
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        var routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RouteResponse> getRoute(@PathVariable UUID id) {
        var route = routeService.getRoute(id);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRoute(@PathVariable UUID id) {
        routeService.deleteRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/prices")
    public ResponseEntity<List<PriceSnapshotResponse>> getPriceHistory(@PathVariable UUID id) {
        var prices = anomalyDetectionService.getSnapshotsForRoute(id);
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/{id}/statistics")
    public ResponseEntity<RouteStatisticsResponse> getStatistics(@PathVariable UUID id) {
        var statistics = anomalyDetectionService.getStatistics(id);
        return ResponseEntity.ok(statistics);
    }
}
