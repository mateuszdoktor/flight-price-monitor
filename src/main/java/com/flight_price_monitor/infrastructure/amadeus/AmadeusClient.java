package com.flight_price_monitor.infrastructure.amadeus;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import com.flight_price_monitor.infrastructure.amadeus.dto.AmadeusFlightOffersResponse;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Component
public class AmadeusClient {
    private static final Logger log = LoggerFactory.getLogger(AmadeusClient.class);
    private final AmadeusProperties props;
    private final WebClient webClient;
    private final OAuthTokenProvider tokenProvider;
    private final AmadeusResilienceProperties resilienceProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;
    private final Clock clock;
    private final Map<RouteKey, CachedFlightPrice> fallbackCache;

    public AmadeusClient(AmadeusProperties props,
                         WebClient webClient,
                         OAuthTokenProvider tokenProvider,
                         AmadeusResilienceProperties resilienceProperties) {
        this(props, webClient, tokenProvider, resilienceProperties, Clock.systemUTC());
    }

    AmadeusClient(AmadeusProperties props,
                  WebClient webClient,
                  OAuthTokenProvider tokenProvider,
                  AmadeusResilienceProperties resilienceProperties,
                  Clock clock) {
        this.props = props;
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
        this.resilienceProperties = resilienceProperties;
        this.clock = clock;
        this.fallbackCache = new ConcurrentHashMap<>();

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(Math.max(1, resilienceProperties.retryMaxAttempts()))
                .waitDuration(Duration.ofMillis(Math.max(0L, resilienceProperties.retryWaitDurationMs())))
                .retryOnException(this::isRetryableException)
                .build();
        this.retry = Retry.of("amadeus-flight-offers", retryConfig);

        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold((float) resilienceProperties.circuitBreakerFailureRateThreshold())
                .slidingWindowSize(Math.max(1, resilienceProperties.circuitBreakerSlidingWindowSize()))
                .minimumNumberOfCalls(Math.max(1, resilienceProperties.circuitBreakerMinimumNumberOfCalls()))
                .waitDurationInOpenState(Duration.ofMillis(Math.max(1L, resilienceProperties.circuitBreakerWaitDurationMs())))
                .recordException(this::isCircuitBreakerFailure)
                .build();
        this.circuitBreaker = CircuitBreaker.of("amadeus-flight-offers", circuitBreakerConfig);

        this.timeLimiter = TimeLimiter.of(Duration.ofMillis(Math.max(1L, resilienceProperties.timeoutMs())));
    }

    public FlightPrice getLowestPrice(String origin, String destination, LocalDate departureDate) {
        RouteKey routeKey = new RouteKey(origin, destination, departureDate);
        Supplier<FlightPrice> resilientCall = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(circuitBreaker, () -> fetchLowestPriceFromApi(origin, destination, departureDate))
        );

        try {
            FlightPrice result = timeLimiter.executeFutureSupplier(() ->
                    CompletableFuture.supplyAsync(resilientCall::get)
            );
            fallbackCache.put(routeKey, new CachedFlightPrice(result, clock.instant()));
            return result;
        } catch (Exception ex) {
            return getFallbackOrThrow(routeKey, ex);
        }
    }

    private FlightPrice fetchLowestPriceFromApi(String origin, String destination, LocalDate departureDate) {
        String token = tokenProvider.getToken();
        AmadeusFlightOffersResponse response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(props.getFlightOffersUrl())
                        .queryParam("originLocationCode", origin)
                        .queryParam("destinationLocationCode", destination)
                        .queryParam("departureDate", departureDate)
                        .queryParam("adults", 1)
                        .queryParam("nonStop", false)
                        .queryParam("max", 1)
                        .queryParam("currencyCode", "EUR")
                        .build()
                ).headers(h -> h.setBearerAuth(token))
                .retrieve()
                .onStatus(HttpStatusCode::isError, res ->
                        res.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(
                                        new AmadeusApiException("Fetching offer error" + errorBody, res.statusCode().value()))
                                )
                )
                .bodyToMono(AmadeusFlightOffersResponse.class)
                .flatMap(res -> {
                    if (res.data() == null || res.data().isEmpty()) {
                        return Mono.error(new AmadeusApiException("No flights found for given criteria", 404));
                    }
                    return Mono.just(res);
                })
                .block();

        if (response == null || response.data() == null || response.data().isEmpty()) {
            throw new AmadeusApiException("No flights found for given criteria", 404);
        }

        var firstOfferPrice = response.data().getFirst().price();

        BigDecimal price = new BigDecimal(firstOfferPrice.grandTotal());
        String currency = firstOfferPrice.currency();

        log.info("Found lowest price: {} {}", price, currency);
        return new FlightPrice(price, currency);
    }

    private FlightPrice getFallbackOrThrow(RouteKey routeKey, Exception failure) {
        CachedFlightPrice fallback = fallbackCache.get(routeKey);
        if (fallback != null) {
            Instant validUntil = fallback.cachedAt().plus(Duration.ofMinutes(Math.max(0L, resilienceProperties.fallbackMaxAgeMinutes())));
            if (clock.instant().isBefore(validUntil)) {
                log.warn(
                        "Using fallback flight price for route {}->{} on {} due to upstream failure",
                        routeKey.origin(),
                        routeKey.destination(),
                        routeKey.departureDate()
                );
                return fallback.price();
            }
        }

        throw toAmadeusException(failure);
    }

    private AmadeusApiException toAmadeusException(Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        if (rootCause instanceof AmadeusApiException amadeusApiException) {
            return amadeusApiException;
        }
        if (rootCause instanceof TimeoutException) {
            return new AmadeusApiException("Amadeus request timed out", rootCause, 504);
        }
        if (rootCause instanceof CallNotPermittedException) {
            return new AmadeusApiException("Amadeus API temporarily unavailable (circuit open)", rootCause, 503);
        }
        return new AmadeusApiException("Amadeus API request failed", rootCause, 502);
    }

    private boolean isRetryableException(Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        if (rootCause instanceof AmadeusApiException amadeusApiException) {
            int statusCode = amadeusApiException.getStatusCode();
            return statusCode == 429 || statusCode >= 500;
        }
        return rootCause instanceof TimeoutException
                || rootCause instanceof IOException
                || rootCause instanceof WebClientRequestException;
    }

    private boolean isCircuitBreakerFailure(Throwable throwable) {
        Throwable rootCause = unwrap(throwable);
        if (rootCause instanceof AmadeusApiException amadeusApiException) {
            int statusCode = amadeusApiException.getStatusCode();
            return statusCode == 429 || statusCode >= 500;
        }
        return rootCause instanceof TimeoutException
                || rootCause instanceof IOException
                || rootCause instanceof WebClientRequestException;
    }

    private Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException || current instanceof ExecutionException) {
            if (current.getCause() == null) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private record RouteKey(String origin, String destination, LocalDate departureDate) {
    }

    private record CachedFlightPrice(FlightPrice price, Instant cachedAt) {
    }

}
