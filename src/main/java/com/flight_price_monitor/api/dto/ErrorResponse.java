package com.flight_price_monitor.api.dto;

import java.time.OffsetDateTime;

public record ErrorResponse(int status, String message, OffsetDateTime timestamp) {
}
