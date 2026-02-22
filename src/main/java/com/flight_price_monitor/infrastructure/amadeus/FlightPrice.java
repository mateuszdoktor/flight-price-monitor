package com.flight_price_monitor.infrastructure.amadeus;

import java.math.BigDecimal;

public record FlightPrice(BigDecimal price, String currency) {
}
