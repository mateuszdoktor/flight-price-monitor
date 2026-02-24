package com.flight_price_monitor.application;

import com.flight_price_monitor.api.dto.CreateRouteRequest;
import com.flight_price_monitor.api.dto.RouteResponse;
import com.flight_price_monitor.common.exception.DuplicateRouteException;
import com.flight_price_monitor.common.exception.RouteNotFoundException;
import com.flight_price_monitor.persistence.entity.RouteEntity;
import com.flight_price_monitor.persistence.mapper.RouteMapper;
import com.flight_price_monitor.persistence.repository.RouteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RouteService {
    private final RouteRepository routeRepository;
    private final RouteMapper routeMapper;

    public RouteService(RouteRepository routeRepository, RouteMapper routeMapper) {
        this.routeRepository = routeRepository;
        this.routeMapper = routeMapper;
    }

    @Transactional
    public RouteResponse createRoute(CreateRouteRequest request) {
        String origin = request.origin().toUpperCase();
        String destination = request.destination().toUpperCase();
        LocalDate departureDate = request.departureDate();

        Optional<RouteEntity> foundRoute = routeRepository.findByOriginAndDestinationAndDepartureDate(
                origin,
                destination,
                departureDate
        );

        if (foundRoute.isPresent() && foundRoute.get().getActive()) {
            throw new DuplicateRouteException(origin, destination, departureDate);
        } else if (foundRoute.isPresent()) {
            RouteEntity route = foundRoute.get();
            route.setActive(true);
            RouteEntity savedRoute = routeRepository.save(route);
            return routeMapper.toResponse(savedRoute);
        }

        RouteEntity route = routeMapper.toEntity(request);
        RouteEntity savedRoute = routeRepository.save(route);
        return routeMapper.toResponse(savedRoute);
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getAllRoutes() {
        return routeRepository.findAll().stream()
                .map(routeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RouteResponse> getActiveRoutes() {
        return routeRepository.findAllByActiveTrue().stream()
                .map(routeMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RouteResponse getRoute(UUID id) {
        RouteEntity foundRoute = routeRepository.findById(id)
                .orElseThrow(() -> new RouteNotFoundException(id));
        return routeMapper.toResponse(foundRoute);
    }

    @Transactional
    public void deleteRoute(UUID id) {
        var foundRoute = routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
        routeRepository.delete(foundRoute);
    }

    @Transactional
    public void deactivateRoute(UUID id) {
        var foundRoute = routeRepository.findById(id).orElseThrow(() -> new RouteNotFoundException(id));
        foundRoute.setActive(false);
        routeRepository.save(foundRoute);
    }
}
