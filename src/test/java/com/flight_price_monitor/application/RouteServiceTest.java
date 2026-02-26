package com.flight_price_monitor.application;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.common.exception.DuplicateRouteException;
import com.flight_price_monitor.common.exception.RouteNotFoundException;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.mapper.RouteMapper;
import com.flight_price_monitor.persistence.repository.RouteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    private static final LocalDate FUTURE_DATE = LocalDate.now().plusMonths(3);
    @Mock
    RouteRepository routeRepository;
    @Mock
    RouteMapper routeMapper;

    // Helpers
    @InjectMocks
    RouteService routeService;

    private RouteEntity buildRouteEntity(UUID id, String origin, String destination, boolean active) {
        RouteEntity e = new RouteEntity();
        e.setId(id);
        e.setOrigin(origin);
        e.setDestination(destination);
        e.setDepartureDate(FUTURE_DATE);
        e.setActive(active);
        e.setCreatedAt(OffsetDateTime.now());
        return e;
    }

    private RouteResponse buildRouteResponse(UUID id, String origin, String destination) {
        return new RouteResponse(id, origin, destination, FUTURE_DATE, true, OffsetDateTime.now());
    }

    @Test
    void createRoute_newRoute_savesAndReturnsResponse() {
        CreateRouteRequest request = new CreateRouteRequest("KR", "BAH", FUTURE_DATE);
        UUID id = UUID.randomUUID();

        RouteEntity newEntity = buildRouteEntity(id, "KR", "BAH", true);
        RouteResponse expected = buildRouteResponse(id, "KR", "BAH");

        when(routeRepository.findByOriginAndDestinationAndDepartureDate("KR", "BAH", FUTURE_DATE))
                .thenReturn(Optional.empty());
        when(routeMapper.toEntity(request)).thenReturn(newEntity);
        when(routeRepository.save(newEntity)).thenReturn(newEntity);
        when(routeMapper.toResponse(newEntity)).thenReturn(expected);

        RouteResponse result = routeService.createRoute(request);

        assertNotNull(result);
        assertEquals("KR", result.origin());
        assertEquals("BAH", result.destination());
        verify(routeRepository, times(1)).save(newEntity);
    }

    @Test
    void createRoute_duplicateActive_throwsDuplicateRouteException() {
        CreateRouteRequest request = new CreateRouteRequest("KR", "BAH", FUTURE_DATE);
        RouteEntity existing = buildRouteEntity(UUID.randomUUID(), "KR", "BAH", true);

        when(routeRepository.findByOriginAndDestinationAndDepartureDate("KR", "BAH", FUTURE_DATE))
                .thenReturn(Optional.of(existing));

        assertThrows(DuplicateRouteException.class, () -> routeService.createRoute(request));
        verify(routeRepository, never()).save(any());
    }

    @Test
    void createRoute_duplicateInactive_reactivatesRoute() {
        CreateRouteRequest request = new CreateRouteRequest("KR", "BAH", FUTURE_DATE);
        UUID id = UUID.randomUUID();
        RouteEntity existing = buildRouteEntity(id, "KR", "BAH", false);
        RouteResponse expected = buildRouteResponse(id, "KR", "BAH");

        when(routeRepository.findByOriginAndDestinationAndDepartureDate("KR", "BAH", FUTURE_DATE))
                .thenReturn(Optional.of(existing));
        when(routeRepository.save(existing)).thenReturn(existing);
        when(routeMapper.toResponse(existing)).thenReturn(expected);

        RouteResponse result = routeService.createRoute(request);

        assertTrue(existing.getActive(), "inactive route should be reactivated");
        assertNotNull(result);
        verify(routeRepository, times(1)).save(existing);
        verify(routeMapper, never()).toEntity(any());
    }

    @Test
    void getRoute_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        RouteEntity entity = buildRouteEntity(id, "KR", "BAH", true);
        RouteResponse expected = buildRouteResponse(id, "KR", "BAH");

        when(routeRepository.findById(id)).thenReturn(Optional.of(entity));
        when(routeMapper.toResponse(entity)).thenReturn(expected);

        RouteResponse result = routeService.getRoute(id);

        assertEquals(expected, result);
        verify(routeMapper, times(1)).toResponse(entity);
    }

    @Test
    void getRoute_nonExistingId_throwsRouteNotFoundException() {
        UUID id = UUID.randomUUID();

        when(routeRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(RouteNotFoundException.class, () -> routeService.getRoute(id));
    }

    @Test
    void deleteRoute_existingId_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        RouteEntity entity = buildRouteEntity(id, "KR", "BAH", true);

        when(routeRepository.findById(id)).thenReturn(Optional.of(entity));
        doNothing().when(routeRepository).delete(entity);

        routeService.deleteRoute(id);

        verify(routeRepository, times(1)).delete(entity);
    }

    @Test
    void getAllRoutes_returnsAllMapped() {
        UUID id = UUID.randomUUID();
        RouteEntity entity = buildRouteEntity(id, "KR", "BAH", true);
        RouteResponse expected = buildRouteResponse(id, "KR", "BAH");

        UUID id1 = UUID.randomUUID();
        RouteEntity entity1 = buildRouteEntity(id1, "KRK", "LHR", true);
        RouteResponse expected1 = buildRouteResponse(id, "KRK", "LHR");

        when(routeRepository.findAll()).thenReturn(List.of(entity, entity1));
        when(routeMapper.toResponse(entity)).thenReturn(expected);
        when(routeMapper.toResponse(entity1)).thenReturn(expected1);


        assertTrue(routeService.getAllRoutes().containsAll(List.of(expected, expected1)));
    }
}
