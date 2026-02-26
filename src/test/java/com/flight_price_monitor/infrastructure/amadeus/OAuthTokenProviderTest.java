package com.flight_price_monitor.infrastructure.amadeus;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import com.flight_price_monitor.common.exception.AmadeusApiException;

import io.netty.channel.ChannelOption;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.netty.http.client.HttpClient;

class OAuthTokenProviderTest {

    private MockWebServer mockWebServer;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() {
        try {
            mockWebServer.shutdown();
        } catch (IOException ignored) {
        }
    }

    private OAuthTokenProvider buildProvider(int expiresIn) {
        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)))
                .build();

        AmadeusProperties props = new AmadeusProperties("key", "secret", baseUrl);
        mockWebServer.enqueue(tokenResponse(expiresIn));
        return new OAuthTokenProvider(webClient, props);
    }

    private MockResponse tokenResponse(int expiresIn) {
        return new MockResponse()
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {
                          "access_token": "my-access-token",
                          "token_type": "Bearer",
                          "expires_in": %d
                        }
                        """.formatted(expiresIn));
    }

    @Test
    void getToken_firstCall_fetchesNewToken() throws InterruptedException {
        OAuthTokenProvider provider = buildProvider(1799);

        String token = provider.getToken();

        assertEquals("my-access-token", token);

        RecordedRequest request = mockWebServer.takeRequest();
        assertEquals("POST", request.getMethod());
        assertTrue(request.getPath().contains("/v1/security/oauth2/token"));

        String body = request.getBody().readUtf8();
        assertTrue(body.contains("grant_type=client_credentials"));
        assertTrue(body.contains("client_id=key"));
        assertTrue(body.contains("client_secret=secret"));
    }

    @Test
    void getToken_cachedToken_returnsCachedWithoutFetching() throws InterruptedException {
        OAuthTokenProvider provider = buildProvider(1799);

        String first  = provider.getToken();
        String second = provider.getToken();
        String third  = provider.getToken();

        assertEquals("my-access-token", first);
        assertEquals("my-access-token", second);
        assertEquals("my-access-token", third);

        assertEquals(1, mockWebServer.getRequestCount(),
                "Cached token must not trigger additional HTTP requests");
    }

    @Test
    void getToken_expiredToken_fetchesNewToken() throws InterruptedException {
        OAuthTokenProvider provider = buildProvider(61);

        String first = provider.getToken();
        assertEquals("my-access-token", first);

        Thread.sleep(1_200);

        mockWebServer.enqueue(tokenResponse(1799));
        String second = provider.getToken();

        assertEquals("my-access-token", second);
        assertEquals(2, mockWebServer.getRequestCount(),
                "Expired cache must trigger a second HTTP request");
    }

    @Test
    void getToken_serverError_throwsAmadeusApiException() {
        String baseUrl = mockWebServer.url("").toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5_000)))
                .build();

        AmadeusProperties props = new AmadeusProperties("key", "secret", baseUrl);
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"error\": \"Unauthorized\"}"));

        OAuthTokenProvider provider = new OAuthTokenProvider(webClient, props);

        assertThrows(AmadeusApiException.class, provider::getToken);
    }
}
