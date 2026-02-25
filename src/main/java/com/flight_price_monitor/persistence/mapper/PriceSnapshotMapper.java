package com.flight_price_monitor.persistence.mapper;

import com.flight_price_monitor.api.dto.PriceSnapshotResponse;
import com.flight_price_monitor.domain.model.PriceSnapshot;
import com.flight_price_monitor.persistence.entity.PriceSnapshotEntity;
import org.springframework.stereotype.Component;

@Component
public class PriceSnapshotMapper {

    public PriceSnapshotResponse toResponse(PriceSnapshotEntity entity) {
        return new PriceSnapshotResponse(
                entity.getId(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getRetrievedAt(),
                entity.getIsAnomaly()
        );
    }

    public PriceSnapshot toDomain(PriceSnapshotEntity entity) {
        return new PriceSnapshot(
                entity.getId(),
                entity.getRoute() != null ? entity.getRoute().getId() : null,
                entity.getPrice(),
                entity.getCurrency(),
                entity.getRetrievedAt(),
                entity.getIsAnomaly()
        );
    }
}

