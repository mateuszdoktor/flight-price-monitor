package com.flight_price_monitor.common.exception;

import java.time.LocalDate;

public class DuplicateRouteException extends RuntimeException {
    public DuplicateRouteException(String origin, String destination, LocalDate departureDate) {
        super("Route already exists: origin=" + origin + ", destination=" + destination + ", departureDate=" + departureDate);
    }
}
