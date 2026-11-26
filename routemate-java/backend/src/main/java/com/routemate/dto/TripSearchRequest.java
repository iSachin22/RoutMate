package com.routemate.dto;

/**
 * POST /api/v1/trips/search — corridor search.
 * Finds trips passing near BOTH points, with pickup before drop along the route.
 */
public record TripSearchRequest(
        Double pickupLat,
        Double pickupLng,
        Double dropLat,
        Double dropLng) {
}
