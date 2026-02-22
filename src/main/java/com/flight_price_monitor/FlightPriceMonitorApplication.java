package com.flight_price_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class FlightPriceMonitorApplication {

    static void main(String[] args) {
        SpringApplication.run(FlightPriceMonitorApplication.class, args);
    }
}
