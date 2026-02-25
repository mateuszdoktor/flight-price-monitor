package com.flight_price_monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableJpaAuditing(dateTimeProviderRef = "offsetDateTimeProvider")
public class FlightPriceMonitorApplication {

    static void main(String[] args) {
        SpringApplication.run(FlightPriceMonitorApplication.class, args);
    }
}
