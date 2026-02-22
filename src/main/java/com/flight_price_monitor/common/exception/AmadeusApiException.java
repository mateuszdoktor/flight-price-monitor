package com.flight_price_monitor.common.exception;

public class AmadeusApiException extends RuntimeException {
    private final int statusCode;

    public AmadeusApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public AmadeusApiException(String message, Throwable cause, int statusCode) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
