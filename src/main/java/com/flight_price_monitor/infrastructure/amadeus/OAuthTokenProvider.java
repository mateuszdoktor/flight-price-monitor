package com.flight_price_monitor.infrastructure.amadeus;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import com.flight_price_monitor.infrastructure.amadeus.dto.AmadeusTokenResponse;

import reactor.core.publisher.Mono;

@Component
public class OAuthTokenProvider {
    private final AmadeusProperties amadeusProperties;
    private final WebClient webClient;
        private final Clock clock;

        private String cachedToken;
        private Instant cachedTokenValidUntil = Instant.EPOCH;

    public OAuthTokenProvider(WebClient webClient, AmadeusProperties amadeusProperties) {
                this(webClient, amadeusProperties, Clock.systemUTC());
        }

        OAuthTokenProvider(WebClient webClient, AmadeusProperties amadeusProperties, Clock clock) {
        this.webClient = webClient;
        this.amadeusProperties = amadeusProperties;
                this.clock = clock;
        }

        public synchronized String getToken() {
                Instant now = clock.instant();
                if (cachedToken != null && now.isBefore(cachedTokenValidUntil)) {
                        return cachedToken;
                }

                AmadeusTokenResponse tokenResponse = webClient.post()
                .uri(amadeusProperties.getTokenUrl())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "client_credentials")
                        .with("client_id", amadeusProperties.apiKey())
                        .with("client_secret", amadeusProperties.apiSecret()))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AmadeusApiException("Fetching token error" + errorBody, res.statusCode().value()))
                                )
                )
                .bodyToMono(AmadeusTokenResponse.class)
                .onErrorMap(e -> !(e instanceof AmadeusApiException),
                        e -> new AmadeusApiException("Amadeus network error", e, 500))
                .block();

        if (tokenResponse == null) {
            throw new AmadeusApiException("Empty token response from Amadeus", 502);
        }

        long cacheSeconds = Math.max(tokenResponse.expiresIn() - 60L, 0L);
        cachedToken = tokenResponse.accessToken();
        cachedTokenValidUntil = clock.instant().plus(Duration.ofSeconds(cacheSeconds));
        return cachedToken;
    }

}
