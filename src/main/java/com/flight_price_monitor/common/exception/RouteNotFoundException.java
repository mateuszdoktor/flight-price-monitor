package com.flight_price_monitor.common.exception;

import java.util.UUID;

public class RouteNotFoundException extends RuntimeException {
    public RouteNotFoundException(UUID routeId) {
        super("Route not found: " + routeId);
    }
}
