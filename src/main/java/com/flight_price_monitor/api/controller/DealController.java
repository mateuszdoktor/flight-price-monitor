package com.flight_price_monitor.api.controller;

import com.flight_price_monitor.api.dto.DealResponse;
import com.flight_price_monitor.api.dto.ErrorResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/deals")
@Tag(name = "Deals", description = "Discover currently detected anomalous flight deals")
public class DealController {
    private final AnomalyDetectionService anomalyDetectionService;

    public DealController(AnomalyDetectionService anomalyDetectionService) {
        this.anomalyDetectionService = anomalyDetectionService;
    }

    @GetMapping
        @Operation(summary = "Get current deals", description = "Returns routes whose latest price is considered anomalously low")
        @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deals returned",
                content = @Content(array = @ArraySchema(schema = @Schema(implementation = DealResponse.class)))),
            @ApiResponse(responseCode = "502", description = "External provider error",
                content = @Content(schema = @Schema(implementation = ErrorResponse.class),
                    examples = @ExampleObject(value = "{\"status\":502,\"message\":\"Amadeus API request failed\",\"timestamp\":\"2026-04-18T12:30:00Z\"}")))
        })
    public ResponseEntity<List<DealResponse>> getDeals() {
        var deals = anomalyDetectionService.getCurrentDeals();
        return ResponseEntity.ok(deals);
    }
}
