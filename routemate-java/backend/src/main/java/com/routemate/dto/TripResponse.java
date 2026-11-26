package com.routemate.dto;

import com.routemate.domain.Trip;
import com.routemate.domain.TripStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Trip payload returned to clients (contract fields + routeWaypoints for UI display). */
public record TripResponse(
        UUID tripId,
        String driverName,
        String originName,
        String destinationName,
        int availableSeats,
        double availableBootKg,
        BigDecimal baseFare,
        Instant departureTime,
        TripStatus status,
        List<WaypointDto> routeWaypoints) {

    public static TripResponse from(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getDriver().getName(),
                trip.getOriginName(),
                trip.getDestinationName(),
                trip.getAvailableSeats(),
                trip.getAvailableBootKg(),
                trip.getBaseFare(),
                trip.getDepartureTime(),
                trip.getStatus(),
                trip.getRouteWaypoints().stream()
                        .map(w -> new WaypointDto(w.getLng(), w.getLat()))
                        .toList());
    }
}
