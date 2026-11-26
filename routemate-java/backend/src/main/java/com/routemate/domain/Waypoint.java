package com.routemate.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * A single route coordinate pair (GeoJSON order: lng, lat — WGS84 / SRID 4326).
 * Stored in the {@code trip_waypoints} element-collection table.
 */
@Embeddable
public class Waypoint {

    @Column(name = "lng", nullable = false)
    private double lng;

    @Column(name = "lat", nullable = false)
    private double lat;

    public Waypoint() {
    }

    public Waypoint(double lng, double lat) {
        this.lng = lng;
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }
}
