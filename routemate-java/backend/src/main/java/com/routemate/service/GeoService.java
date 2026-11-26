package com.routemate.service;

import com.routemate.domain.Waypoint;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Pure-Java geospatial maths (no PostGIS needed):
 *  · haversine distance,
 *  · distance from a point to the trip polyline,
 *  · fractional progress (0..1) of a point along the polyline.
 * Used by the corridor search: pickup and drop must both lie close to the
 * route AND pickup must come before drop along it.
 */
@Service
public class GeoService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    /** Great-circle distance in km. */
    public double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));
    }

    /**
     * Minimum distance (km) from the point to the polyline formed by the
     * ordered waypoints, computed in a local equirectangular projection.
     */
    public double distanceToRouteKm(double lat, double lng, List<Waypoint> route) {
        Projection p = project(lat, lng, route);
        double best = Double.MAX_VALUE;
        for (int i = 0; i < route.size() - 1; i++) {
            Projection a = project(route.get(i).getLat(), route.get(i).getLng(), route);
            Projection b = project(route.get(i + 1).getLat(), route.get(i + 1).getLng(), route);
            double[] closest = closestPointOnSegment(p.x(), p.y(), a.x(), a.y(), b.x(), b.y());
            double dist = Math.hypot(p.x() - closest[0], p.y() - closest[1]);
            best = Math.min(best, dist);
        }
        return best / 1000.0; // metres → km
    }

    /**
     * Fractional progress of the point along the route (0 = at the origin,
     * 1 = at the destination), based on where its closest route point falls.
     */
    public double progressAlongRoute(double lat, double lng, List<Waypoint> route) {
        Projection p = project(lat, lng, route);
        double totalLength = 0;
        double travelledToBest = 0;
        double bestDist = Double.MAX_VALUE;

        double cumulative = 0;
        for (int i = 0; i < route.size() - 1; i++) {
            Projection a = project(route.get(i).getLat(), route.get(i).getLng(), route);
            Projection b = project(route.get(i + 1).getLat(), route.get(i + 1).getLng(), route);
            double segLen = Math.hypot(b.x() - a.x(), b.y() - a.y());
            double[] closest = closestPointOnSegment(p.x(), p.y(), a.x(), a.y(), b.x(), b.y());
            double dist = Math.hypot(p.x() - closest[0], p.y() - closest[1]);
            if (dist < bestDist) {
                bestDist = dist;
                double t = segLen == 0 ? 0 : Math.hypot(closest[0] - a.x(), closest[1] - a.y()) / segLen;
                travelledToBest = cumulative + t * segLen;
            }
            cumulative += segLen;
            totalLength = cumulative;
        }
        if (totalLength == 0) return 0;
        return Math.max(0.0, Math.min(1.0, travelledToBest / totalLength));
    }

    /* ---- local equirectangular projection (good enough at regional scale) ---- */

    private record Projection(double x, double y) {
    }

    private Projection project(double lat, double lng, List<Waypoint> route) {
        double refLat = route.isEmpty() ? lat : route.get(0).getLat();
        double x = Math.toRadians(lng) * EARTH_RADIUS_KM * 1000 * Math.cos(Math.toRadians(refLat));
        double y = Math.toRadians(lat) * EARTH_RADIUS_KM * 1000;
        return new Projection(x, y);
    }

    /** Closest point on segment [a,b] to point p, all in projected metres. */
    private double[] closestPointOnSegment(double px, double py,
                                           double ax, double ay,
                                           double bx, double by) {
        double dx = bx - ax;
        double dy = by - ay;
        double lenSq = dx * dx + dy * dy;
        double t = lenSq == 0 ? 0 : ((px - ax) * dx + (py - ay) * dy) / lenSq;
        t = Math.max(0, Math.min(1, t));
        return new double[]{ax + t * dx, ay + t * dy};
    }
}
