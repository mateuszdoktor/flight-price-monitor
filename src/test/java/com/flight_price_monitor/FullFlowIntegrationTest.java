package com.flight_price_monitor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.fasterxml.jackson.databind.JsonNode;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.repository.PriceSnapshotRepository;
import com.flight_price_monitor.persistence.repository.RouteRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class FullFlowIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
        @SuppressWarnings("unused")
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.task.scheduling.enabled", () -> "false");
        registry.add("amadeus.api-key", () -> "test-key");
        registry.add("amadeus.api-secret", () -> "test-secret");
        registry.add("amadeus.base-url", () -> "http://localhost");
        registry.add("anomaly.min-samples", () -> "5");
        registry.add("anomaly.z-score-threshold", () -> "2.0");
        registry.add("anomaly.percentage-threshold", () -> "0.7");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    RouteRepository routeRepository;

    @Autowired
    PriceSnapshotRepository priceSnapshotRepository;

    @BeforeEach
        @SuppressWarnings("unused")
    void cleanDatabase() {
        priceSnapshotRepository.deleteAll();
        routeRepository.deleteAll();
    }

    @Test
    void routeLifecycle_softDeleteAndReadBack() throws Exception {
        LocalDate departureDate = LocalDate.now().plusDays(30);
                                String body = buildCreateRouteJson("WAW", "LHR", departureDate);

        MvcResult createResult = mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origin").value("WAW"))
                .andExpect(jsonPath("$.active").value(true))
                .andReturn();

        JsonNode createdRoute = com.fasterxml.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(createResult.getResponse().getContentAsString());
        String routeId = createdRoute.path("id").asText();

        mockMvc.perform(delete("/routes/{id}", routeId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/routes/{id}", routeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(routeId))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void dealsAndStatistics_endpointsUsePersistedSnapshots() throws Exception {
        RouteEntity route = new RouteEntity();
        route.setOrigin("KRK");
        route.setDestination("MAD");
        route.setDepartureDate(LocalDate.now().plusDays(45));
        route.setActive(true);
        RouteEntity savedRoute = routeRepository.save(route);

        saveSnapshot(savedRoute, "100.00");
        saveSnapshot(savedRoute, "150.00");
        saveSnapshot(savedRoute, "200.00");
        saveSnapshot(savedRoute, "250.00");
        saveSnapshot(savedRoute, "300.00");
        saveSnapshot(savedRoute, "50.00");

        mockMvc.perform(get("/routes/{id}/statistics", savedRoute.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(savedRoute.getId().toString()))
                .andExpect(jsonPath("$.sampleCount").value(6))
                .andExpect(jsonPath("$.currentPrice").value(50.00));

        mockMvc.perform(get("/deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].routeId").value(savedRoute.getId().toString()))
                .andExpect(jsonPath("$[0].dropPercentage", greaterThan(0.0)));
    }

    @Test
    void creatingDuplicateRoute_returnsConflict() throws Exception {
        LocalDate departureDate = LocalDate.now().plusDays(20);
        String body = buildCreateRouteJson("WAW", "CDG", departureDate);

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    private void saveSnapshot(RouteEntity route, String price) {
        PriceSnapshotEntity snapshot = PriceSnapshotEntity.builder()
                .id(UUID.randomUUID())
                .route(route)
                .price(new BigDecimal(price))
                .currency("EUR")
                .isAnomaly(false)
                .build();
        priceSnapshotRepository.save(snapshot);
    }

        private String buildCreateRouteJson(String origin, String destination, LocalDate departureDate) {
                return "{\"origin\":\"%s\",\"destination\":\"%s\",\"departureDate\":\"%s\"}"
                                .formatted(origin, destination, departureDate);
        }
}
