package com.flight_price_monitor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI flightPriceMonitorOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Flight Price Monitor API")
                        .description("REST API for monitored routes, price snapshots, statistics and flight deals.")
                        .version("v1")
                        .contact(new Contact().name("Flight Price Monitor").url("https://example.com"))
                        .license(new License().name("Educational/Portfolio"))
                );
    }
}
