package com.flight_price_monitor.infrastructure.amadeus.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AmadeusFlightOffersResponse(List<FlightOffer> data) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FlightOffer(FlightOfferPrice price) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        public record FlightOfferPrice(String currency, String total, String grandTotal) {
        }
    }
}
