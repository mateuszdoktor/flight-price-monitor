package com.flight_price_monitor.common.exception;

import java.util.UUID;

public class InsufficientDataException extends RuntimeException {
    public InsufficientDataException(UUID routeId, int required, int actual) {
        super("Route " + routeId + " has " + actual + " snapshots, requires at least " + required);
    }
}
