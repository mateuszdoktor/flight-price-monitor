package com.flight_price_monitor.infrastructure.amadeus;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import com.flight_price_monitor.infrastructure.amadeus.dto.AmadeusTokenResponse;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class OAuthTokenProvider {
    private final AmadeusProperties amadeusProperties;
    private final WebClient webClient;
    private final Mono<String> tokenCache;

    public OAuthTokenProvider(WebClient webClient, AmadeusProperties amadeusProperties) {
        this.webClient = webClient;
        this.amadeusProperties = amadeusProperties;
        this.tokenCache = webClient.post()
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
                .cache(
                        response -> Duration.ofSeconds(response.expiresIn() - 60),
                        error -> Duration.ZERO,
                        () -> Duration.ZERO
                )
                .map(AmadeusTokenResponse::accessToken)
                .onErrorMap(e -> !(e instanceof AmadeusApiException),
                        e -> new AmadeusApiException("Amadeus network error", e, 500));
    }

    public String getToken() {
        return tokenCache.block();
    }

}
