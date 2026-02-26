package com.flight_price_monitor.api.controller;

import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.api.dto.RouteStatisticsResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import com.flight_price_monitor.application.RouteService;
import com.flight_price_monitor.common.exception.RouteNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RouteController.class)
@org.springframework.security.test.context.support.WithMockUser
class RouteControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    RouteService routeService;

    @MockitoBean
    AnomalyDetectionService anomalyDetectionService;

    @MockitoBean
    @SuppressWarnings("unused")
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean(name = "offsetDateTimeProvider")
    @SuppressWarnings("unused")
    DateTimeProvider offsetDateTimeProvider;

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(3);

    private RouteResponse routeResponse(UUID id) {
        return new RouteResponse(id, "WAW", "LHR", FUTURE_DATE, true, OffsetDateTime.now());
    }

    @Test
    void createRoute_validRequest_returns201() throws Exception {
        UUID id = UUID.randomUUID();
        RouteResponse response = routeResponse(id);

        when(routeService.createRoute(any())).thenReturn(response);

        String json = """
                {"origin":"WAW","destination":"LHR","departureDate":"%s"}
                """.formatted(FUTURE_DATE);

        mockMvc.perform(post("/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.origin").value("WAW"))
                .andExpect(jsonPath("$.destination").value("LHR"))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/routes/" + id)));
    }

    @Test
    void createRoute_invalidOrigin_returns400() throws Exception {
        String json = """
                {
                  "origin": "",
                  "destination": "LHR",
                  "departureDate": "%s"
                }
                """.formatted(FUTURE_DATE);

        mockMvc.perform(post("/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoute_missingDate_returns400() throws Exception {
        String json = """
                {
                  "origin": "WAW",
                  "destination": "LHR"
                }
                """;

        mockMvc.perform(post("/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createRoute_pastDate_returns400() throws Exception {
        String json = """
                {
                  "origin": "WAW",
                  "destination": "LHR",
                  "departureDate": "2020-01-01"
                }
                """;

        mockMvc.perform(post("/routes")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllRoutes_returns200WithList() throws Exception {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        when(routeService.getAllRoutes()).thenReturn(List.of(routeResponse(id1), routeResponse(id2)));

        mockMvc.perform(get("/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(id1.toString()))
                .andExpect(jsonPath("$[1].id").value(id2.toString()));
    }

    @Test
    void getRoute_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();

        when(routeService.getRoute(id)).thenReturn(routeResponse(id));

        mockMvc.perform(get("/routes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.origin").value("WAW"));
    }

    @Test
    void getRoute_nonExistingId_returns404() throws Exception {
        UUID id = UUID.randomUUID();

        when(routeService.getRoute(id)).thenThrow(new RouteNotFoundException(id));

        mockMvc.perform(get("/routes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deleteRoute_existingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        doNothing().when(routeService).deactivateRoute(id);

        mockMvc.perform(delete("/routes/{id}", id)
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(routeService, times(1)).deactivateRoute(id);
    }

    @Test
    void getPriceHistory_returns200WithList() throws Exception {
        UUID id = UUID.randomUUID();
        PriceSnapshotResponse snapshot = new PriceSnapshotResponse(
                UUID.randomUUID(), new BigDecimal("199.99"), "EUR", OffsetDateTime.now(), false);

        when(anomalyDetectionService.getSnapshotsForRoute(id)).thenReturn(List.of(snapshot));

        mockMvc.perform(get("/routes/{id}/prices", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].price").value(199.99))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }

    @Test
    void getStatistics_returns200WithStats() throws Exception {
        UUID id = UUID.randomUUID();
        RouteStatisticsResponse stats = RouteStatisticsResponse.builder()
                .routeId(id)
                .origin("WAW")
                .destination("LHR")
                .departureDate(FUTURE_DATE)
                .mean(new BigDecimal("200.00"))
                .median(new BigDecimal("200.00"))
                .standardDeviation(new BigDecimal("50.00"))
                .min(new BigDecimal("100.00"))
                .max(new BigDecimal("300.00"))
                .sampleCount(10)
                .currentPrice(new BigDecimal("180.00"))
                .zScore(-0.4)
                .build();

        when(anomalyDetectionService.getStatistics(id)).thenReturn(stats);

        mockMvc.perform(get("/routes/{id}/statistics", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routeId").value(id.toString()))
                .andExpect(jsonPath("$.origin").value("WAW"))
                .andExpect(jsonPath("$.sampleCount").value(10))
                .andExpect(jsonPath("$.mean").value(200.00));
    }
}
