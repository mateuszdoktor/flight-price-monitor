package com.flight_price_monitor.api.controller;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.ErrorResponse;
import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.api.dto.RouteStatisticsResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import com.flight_price_monitor.application.RouteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/routes")
@Tag(name = "Routes", description = "Manage monitored routes and inspect their historical price data")
public class RouteController {
    private final RouteService routeService;
    private final AnomalyDetectionService anomalyDetectionService;

    public RouteController(RouteService routeService, AnomalyDetectionService anomalyDetectionService) {
        this.routeService = routeService;
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @PostMapping
        @Operation(summary = "Create monitored route", description = "Registers a route to be tracked for price changes")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Route created",
                content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":400,\"message\":\"origin: must not be blank\",\"timestamp\":\"2026-04-18T12:30:00Z\"}"))),
            @ApiResponse(responseCode = "409", description = "Route already exists",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":409,\"message\":\"Route already exists: origin=WAW, destination=LHR, departureDate=2026-08-01\",\"timestamp\":\"2026-04-18T12:30:00Z\"}")))
        })
    public ResponseEntity<RouteResponse> createRoute(@Valid @RequestBody CreateRouteRequest request) {
        var route = routeService.createRoute(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(route.id()).toUri();
        return ResponseEntity.created(location).body(route);
    }

    @GetMapping
        @Operation(summary = "List routes", description = "Returns all monitored routes")
        @ApiResponse(responseCode = "200", description = "Routes returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = RouteResponse.class))))
    public ResponseEntity<List<RouteResponse>> getAllRoutes() {
        var routes = routeService.getAllRoutes();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/{id}")
        @Operation(summary = "Get route details", description = "Returns route metadata by identifier")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Route found",
                content = @Content(schema = @Schema(implementation = RouteResponse.class))),
            @ApiResponse(responseCode = "404", description = "Route not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Route not found: 11111111-1111-1111-1111-111111111111\",\"timestamp\":\"2026-04-18T12:30:00Z\"}")))
        })
    public ResponseEntity<RouteResponse> getRoute(@PathVariable UUID id) {
        var route = routeService.getRoute(id);
        return ResponseEntity.ok(route);
    }

    // DELETE performs soft-delete by deactivating a route.
    @DeleteMapping("/{id}")
        @Operation(summary = "Deactivate route", description = "Soft-delete operation that marks route as inactive")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Route deactivated"),
            @ApiResponse(responseCode = "404", description = "Route not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":404,\"message\":\"Route not found: 11111111-1111-1111-1111-111111111111\",\"timestamp\":\"2026-04-18T12:30:00Z\"}")))
        })
    public ResponseEntity<Void> deactivateRoute(@PathVariable UUID id) {
        routeService.deactivateRoute(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/prices")
        @Operation(summary = "Get route price history", description = "Returns snapshots sorted from newest to oldest")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Snapshots returned",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = PriceSnapshotResponse.class)))),
            @ApiResponse(responseCode = "404", description = "Route not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
        })
    public ResponseEntity<List<PriceSnapshotResponse>> getPriceHistory(@PathVariable UUID id) {
        var prices = anomalyDetectionService.getSnapshotsForRoute(id);
        return ResponseEntity.ok(prices);
    }

    @GetMapping("/{id}/statistics")
        @Operation(summary = "Get route statistics", description = "Returns aggregated statistics for route price history")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics returned",
                content = @Content(schema = @Schema(implementation = RouteStatisticsResponse.class))),
            @ApiResponse(responseCode = "404", description = "Route not found",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "422", description = "Insufficient historical data",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":422,\"message\":\"Route 11111111-1111-1111-1111-111111111111 has 0 snapshots, requires at least 5\",\"timestamp\":\"2026-04-18T12:30:00Z\"}")))
        })
    public ResponseEntity<RouteStatisticsResponse> getStatistics(@PathVariable UUID id) {
        var statistics = anomalyDetectionService.getStatistics(id);
        return ResponseEntity.ok(statistics);
    }
}
