package com.flight_price_monitor.infrastructure.amadeus;

import com.flight_price_monitor.common.exception.AmadeusApiException;
import com.flight_price_monitor.infrastructure.amadeus.dto.AmadeusFlightOffersResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Component
public class AmadeusClient {
    private static final Logger log = LoggerFactory.getLogger(AmadeusClient.class);
    AmadeusProperties props;
    WebClient webClient;
    OAuthTokenProvider tokenProvider;

    public AmadeusClient(AmadeusProperties props, WebClient webClient, OAuthTokenProvider tokenProvider) {
        this.props = props;
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
    }

    public FlightPrice getLowestPrice(String origin, String destination, LocalDate departureDate) {
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

        var firstOfferPrice = response.data().getFirst().price();

        BigDecimal price = new BigDecimal(firstOfferPrice.grandTotal());
        String currency = firstOfferPrice.currency();

        log.info("Found lowest price: {} {}", price, currency);
        return new FlightPrice(price, currency);
    }

}
