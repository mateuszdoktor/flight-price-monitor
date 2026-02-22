package com.flight_price_monitor.infrastructure.amadeus.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record AmadeusTokenResponse(@JsonProperty("access_token") String accessToken,
                                   @JsonProperty("token_type") String tokenType,
                                   @JsonProperty("expires_in") int expiresIn) {
}
