package com.flight_price_monitor.persistence.mapper;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.domain.model.Route;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface RouteMapper {
    RouteResponse toResponse(RouteEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "origin", qualifiedByName = "toUpper")
    @Mapping(target = "destination", qualifiedByName = "toUpper")
    RouteEntity toEntity(CreateRouteRequest createRouteRequest);

    Route toDomain(RouteEntity entity);

    @Named("toUpper")
    default String mapToUpperCase(String value) {
        return value != null ? value.toUpperCase() : null;
    }
}
