package com.routemate.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * POST /api/v1/trips — publish a trip.
 * {@code routeWaypoints} needs at least 2 valid (lng, lat) pairs to form a LineString.
 */
public record TripRequest(
        String driverId,
        String originName,
        String destinationName,
        List<WaypointDto> routeWaypoints,
        Integer availableSeats,
        Double availableBootKg,
        BigDecimal baseFare,
        Instant departureTime) {
}
