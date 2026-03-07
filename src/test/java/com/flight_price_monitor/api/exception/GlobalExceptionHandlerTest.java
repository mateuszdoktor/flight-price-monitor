package com.flight_price_monitor.api.exception;

import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import com.flight_price_monitor.common.exception.DuplicateRouteException;
import com.flight_price_monitor.common.exception.InsufficientDataException;
import com.flight_price_monitor.common.exception.RouteNotFoundException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    @SuppressWarnings("unused")
    JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockitoBean(name = "offsetDateTimeProvider")
    @SuppressWarnings("unused")
    DateTimeProvider offsetDateTimeProvider;

    @Test
    void handlesRouteNotFoundException() throws Exception {
        mockMvc.perform(get("/test/errors/route-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Route not found")));
    }

    @Test
    void handlesDuplicateRouteException() throws Exception {
        mockMvc.perform(get("/test/errors/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Route already exists")));
    }

    @Test
    void handlesInsufficientDataException() throws Exception {
        mockMvc.perform(get("/test/errors/insufficient"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("requires at least")));
    }

    @Test
    void handlesAmadeusExceptionWithKnownStatus() throws Exception {
        mockMvc.perform(get("/test/errors/amadeus/429"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value("amadeus"));
    }

    @Test
    void handlesAmadeusExceptionWithUnknownStatusAsBadGateway() throws Exception {
        mockMvc.perform(get("/test/errors/amadeus/599"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("amadeus"));
    }

    @Test
    void handlesValidationErrors() throws Exception {
        mockMvc.perform(post("/test/errors/validation")
                        .contentType("application/json")
                        .content("{\"origin\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("origin")));
    }

    @Test
    void handlesUnexpectedExceptions() throws Exception {
        mockMvc.perform(get("/test/errors/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @RestController
    @RequestMapping("/test/errors")
    @SuppressWarnings("unused")
    static class ThrowingController {

        @GetMapping("/route-not-found")
        void routeNotFound() {
            throw new RouteNotFoundException(UUID.randomUUID());
        }

        @GetMapping("/duplicate")
        void duplicate() {
            throw new DuplicateRouteException("WAW", "LHR", LocalDate.now().plusDays(1));
        }

        @GetMapping("/insufficient")
        void insufficient() {
            throw new InsufficientDataException(UUID.randomUUID(), 5, 1);
        }

        @GetMapping("/amadeus/{status}")
        void amadeus(@PathVariable int status) {
            throw new AmadeusApiException("amadeus", status);
        }

        @PostMapping("/validation")
        void validation(@Valid @RequestBody ValidationRequest request) {
        }

        @GetMapping("/generic")
        void generic() {
            throw new IllegalStateException("boom");
        }
    }

    record ValidationRequest(@NotBlank String origin) {
    }
}
