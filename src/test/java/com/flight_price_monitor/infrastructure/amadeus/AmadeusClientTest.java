package com.flight_price_monitor.infrastructure.amadeus;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AmadeusClientTest {

    private static final String TOKEN_RESPONSE = """
            {
              "access_token": "test-token-123",
              "token_type": "Bearer",
              "expires_in": 1799
            }
            """;
    private static final String VALID_FLIGHT_OFFERS = """
            {
              "data": [
                {
                  "price": {
                    "currency": "EUR",
                    "total": "250.00",
                    "grandTotal": "275.50"
                  }
                }
              ]
            }
            """;
    private static final String EMPTY_FLIGHT_OFFERS = """
            {
              "data": []
            }
            """;
    private MockWebServer mockWebServer;

    @BeforeEach
        @SuppressWarnings("unused")
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
        @SuppressWarnings("unused")
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException ignored) {
        }
    }

        private AmadeusClient buildClient(int readTimeoutSeconds, int maxAttempts, long timeoutMs) {
        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)
                .doOnConnected(c -> c.addHandlerLast(new ReadTimeoutHandler(readTimeoutSeconds)));

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        AmadeusProperties props = new AmadeusProperties("test-key", "test-secret", baseUrl);
        OAuthTokenProvider tokenProvider = new OAuthTokenProvider(webClient, props);
                AmadeusResilienceProperties resilienceProperties = new AmadeusResilienceProperties(
                                maxAttempts,
                                10,
                                10,
                                2,
                                50,
                                10_000,
                                timeoutMs,
                                60
                );
                return new AmadeusClient(props, webClient, tokenProvider, resilienceProperties);
        }

        private AmadeusClient buildClient(int readTimeoutSeconds) {
                return buildClient(readTimeoutSeconds, 1, 3_000);
    }

    private MockResponse jsonResponse(String body) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody(body);
    }

    @Test
    void getLowestPrice_validResponse_returnsFlightPrice() throws InterruptedException {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(jsonResponse(VALID_FLIGHT_OFFERS));

        FlightPrice result = buildClient(30)
                .getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1));

        assertNotNull(result);
        assertEquals(new BigDecimal("275.50"), result.price());
        assertEquals("EUR", result.currency());

        mockWebServer.takeRequest();
        RecordedRequest flightReq = mockWebServer.takeRequest();
        String authHeader = flightReq.getHeader("Authorization");
        assertNotNull(authHeader);
        assertTrue(authHeader.startsWith("Bearer "),
                "Authorization: Bearer header should be present");
        String requestPath = flightReq.getPath();
        assertNotNull(requestPath);
        assertTrue(requestPath.contains("originLocationCode=KRK"));
        assertTrue(requestPath.contains("destinationLocationCode=BAH"));
    }

    @Test
    void getLowestPrice_emptyData_throwsAmadeusApiException() {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(jsonResponse(EMPTY_FLIGHT_OFFERS));

        AmadeusApiException exception = assertThrows(AmadeusApiException.class,
                () -> buildClient(30).getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1)));
        assertNotNull(exception);
    }

    @Test
    void getLowestPrice_serverError_throwsAmadeusApiException() {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Internal Server Error\"}"));

        AmadeusApiException exception = assertThrows(AmadeusApiException.class,
                () -> buildClient(30).getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1)));
        assertNotNull(exception);
    }

    @Test
    void getLowestPrice_timeout_throwsAmadeusApiException() {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(new MockResponse()
                .setBodyDelay(10, TimeUnit.SECONDS)
                .setBody(VALID_FLIGHT_OFFERS));

        AmadeusApiException exception = assertThrows(
                AmadeusApiException.class,
                () -> buildClient(30, 1, 150).getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1))
        );
        assertEquals(504, exception.getStatusCode());
    }

    @Test
    void getLowestPrice_transientServerError_retriesAndSucceeds() {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"temporary\"}"));
        mockWebServer.enqueue(jsonResponse(VALID_FLIGHT_OFFERS));

        FlightPrice result = buildClient(30, 2, 3_000)
                .getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1));

        assertNotNull(result);
        assertEquals(new BigDecimal("275.50"), result.price());
    }

    @Test
    void getLowestPrice_whenProviderFails_usesRecentFallbackPrice() {
        mockWebServer.enqueue(jsonResponse(TOKEN_RESPONSE));
        mockWebServer.enqueue(jsonResponse(VALID_FLIGHT_OFFERS));
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\":\"upstream down\"}"));

        AmadeusClient client = buildClient(30, 1, 3_000);

        FlightPrice first = client.getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1));
        FlightPrice fallback = client.getLowestPrice("KRK", "BAH", LocalDate.of(2026, 6, 1));

        assertEquals(first.price(), fallback.price());
        assertEquals(first.currency(), fallback.currency());
    }
}
