package com.flight_price_monitor.persistence.repository;

import com.flight_price_monitor.persistence.entity.RouteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RouteRepository extends JpaRepository<RouteEntity, UUID> {
    List<RouteEntity> findAllByActiveTrue();

    Optional<RouteEntity> findByOriginAndDestinationAndDepartureDate(String origin, String destination, LocalDate departureDate);
}
