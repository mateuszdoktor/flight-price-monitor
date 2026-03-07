package com.flight_price_monitor.api.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.flight_price_monitor.api.dto.DealResponse;
import com.flight_price_monitor.application.AnomalyDetectionService;
import com.flight_price_monitor.common.exception.AmadeusApiException;

@WebMvcTest(DealController.class)
class DealControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AnomalyDetectionService anomalyDetectionService;

    @MockitoBean
    @SuppressWarnings("unused")
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean(name = "offsetDateTimeProvider")
    @SuppressWarnings("unused")
    DateTimeProvider offsetDateTimeProvider;

    @Test
    void getDeals_returns200WithDeals() throws Exception {
        DealResponse deal = DealResponse.builder()
                .routeId(UUID.randomUUID())
                .origin("WAW")
                .destination("LHR")
                .departureDate(LocalDate.now().plusMonths(1))
                .currentPrice(new BigDecimal("129.99"))
                .averagePrice(new BigDecimal("219.99"))
                .dropPercentage(new BigDecimal("40.91"))
                .currency("EUR")
                .retrievedAt(OffsetDateTime.now())
                .build();

        when(anomalyDetectionService.getCurrentDeals()).thenReturn(List.of(deal));

        mockMvc.perform(get("/deals"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].origin").value("WAW"))
                .andExpect(jsonPath("$[0].destination").value("LHR"));
    }

    @Test
    void getDeals_whenAmadeusError_returnsMappedStatus() throws Exception {
        when(anomalyDetectionService.getCurrentDeals()).thenThrow(new AmadeusApiException("rate limit", 429));

        mockMvc.perform(get("/deals"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("rate limit"));
    }
}
