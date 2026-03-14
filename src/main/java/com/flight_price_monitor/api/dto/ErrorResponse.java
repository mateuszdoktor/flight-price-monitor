package com.flight_price_monitor.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.OffsetDateTime;

@Schema(description = "Standard error response returned by the API")
public record ErrorResponse(
	@Schema(description = "HTTP status code", example = "404")
	int status,
	@Schema(description = "Human-readable error message", example = "Route not found: 11111111-1111-1111-1111-111111111111")
	String message,
	@Schema(description = "Timestamp when the error was generated", example = "2026-04-18T12:30:00Z")
	OffsetDateTime timestamp
) {
}
