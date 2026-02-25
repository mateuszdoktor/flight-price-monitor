package com.flight_price_monitor.persistence.mapper;

import org.springframework.stereotype.Component;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.domain.model.Route;
import com.flight_price_monitor.persistence.entity.RouteEntity;

@Component
public class RouteMapper {

    public RouteResponse toResponse(RouteEntity entity) {
        return new RouteResponse(
                entity.getId(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getDepartureDate(),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }

    public RouteEntity toEntity(CreateRouteRequest request) {
        RouteEntity entity = new RouteEntity();
        entity.setOrigin(request.origin().toUpperCase());
        entity.setDestination(request.destination().toUpperCase());
        entity.setDepartureDate(request.departureDate());
        entity.setActive(true);
        return entity;
    }

    public Route toDomain(RouteEntity entity) {
        return new Route(
                entity.getId(),
                entity.getOrigin(),
                entity.getDestination(),
                entity.getDepartureDate(),
                entity.getActive(),
                entity.getCreatedAt()
        );
    }
}
