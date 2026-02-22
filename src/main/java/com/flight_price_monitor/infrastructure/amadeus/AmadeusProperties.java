package com.flight_price_monitor.infrastructure.amadeus;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "amadeus")
public record AmadeusProperties(String apiKey, String apiSecret, String baseUrl) {
    public String getTokenUrl() {
        return "/v1/security/oauth2/token";
    }

    public String getFlightOffersUrl() {
        return "/v2/shopping/flight-offers";
    }
}
