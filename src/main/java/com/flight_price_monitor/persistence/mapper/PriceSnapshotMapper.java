package com.flight_price_monitor.persistence.mapper;

import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.domain.model.PriceSnapshot;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PriceSnapshotMapper {
    @Mapping(source = "isAnomaly", target = "anomaly")
    PriceSnapshotResponse toResponse(PriceSnapshotEntity entity);

    @Mapping(target = "routeId", expression = "java(entity.getRoute() != null ? entity.getRoute().getId() : null)")
    PriceSnapshot toDomain(PriceSnapshotEntity entity);
}

