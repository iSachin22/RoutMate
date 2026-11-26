package com.routemate.dto;

/**
 * One route coordinate pair in GeoJSON order (lng, lat — WGS84 / SRID 4326).
 * Range validation happens in the service layer with explicit messages.
 */
public record WaypointDto(Double lng, Double lat) {
}
