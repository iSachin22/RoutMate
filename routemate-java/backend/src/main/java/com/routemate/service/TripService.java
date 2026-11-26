package com.routemate.service;

import com.routemate.domain.Trip;
import com.routemate.domain.User;
import com.routemate.domain.Waypoint;
import com.routemate.dto.TripRequest;
import com.routemate.dto.TripResponse;
import com.routemate.dto.TripSearchRequest;
import com.routemate.dto.WaypointDto;
import com.routemate.exception.ApiException;
import com.routemate.repository.TripRepository;
import com.routemate.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/** Publish trips, list driver trips, corridor search. */
@Service
public class TripService {

    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final GeoService geoService;
    private final double corridorKm;

    public TripService(TripRepository tripRepository,
                       UserRepository userRepository,
                       GeoService geoService,
                       @Value("${routemate.search.corridor-km:50}") double corridorKm) {
        this.tripRepository = tripRepository;
        this.userRepository = userRepository;
        this.geoService = geoService;
        this.corridorKm = corridorKm;
    }

    /** Publishes a trip. Enforces the LineString contract (&gt;= 2 valid coordinate pairs). */
    @Transactional
    public TripResponse publishTrip(TripRequest request, UUID authenticatedDriverId) {
        validateTripPayload(request);

        // driverId in the payload is informational — the JWT decides who drives.
        if (request.driverId() != null && !request.driverId().isBlank()
                && !UUID.fromString(request.driverId()).equals(authenticatedDriverId)) {
            throw ApiException.forbidden("Unauthorized / Role Mismatch: driverId does not match the authenticated driver.");
        }

        User driver = userRepository.findById(authenticatedDriverId)
                .orElseThrow(() -> ApiException.notFound("Driver not found."));

        Trip trip = new Trip();
        trip.setDriver(driver);
        trip.setOriginName(request.originName().trim());
        trip.setDestinationName(request.destinationName().trim());
        trip.setRouteWaypoints(request.routeWaypoints().stream()
                .map(w -> new Waypoint(w.lng(), w.lat()))
                .toList());
        trip.setAvailableSeats(request.availableSeats());
        trip.setAvailableBootKg(request.availableBootKg());
        trip.setBaseFare(request.baseFare());
        trip.setDepartureTime(request.departureTime());
        tripRepository.save(trip);
        return TripResponse.from(trip);
    }

    @Transactional(readOnly = true)
    public List<TripResponse> getDriverTrips(UUID driverId, UUID authenticatedDriverId) {
        if (!driverId.equals(authenticatedDriverId)) {
            throw ApiException.forbidden("Unauthorized / Role Mismatch: drivers can only view their own trips.");
        }
        return tripRepository.findByDriverId(driverId).stream()
                .map(TripResponse::from)
                .toList();
    }

    /**
     * Corridor search: trips passing within {@code corridorKm} of BOTH pickup and
     * drop, with pickup before drop along the route polyline.
     */
    @Transactional(readOnly = true)
    public List<TripResponse> search(TripSearchRequest request) {
        validateCoordinate(request.pickupLat(), request.pickupLng(), "pickup");
        validateCoordinate(request.dropLat(), request.dropLng(), "drop");

        return tripRepository.findAllScheduled().stream()
                .filter(trip -> trip.getAvailableSeats() > 0 || trip.getAvailableBootKg() > 0)
                .filter(trip -> {
                    List<Waypoint> route = trip.getRouteWaypoints();
                    if (route.size() < 2) return false;
                    boolean pickupNear = geoService.distanceToRouteKm(request.pickupLat(), request.pickupLng(), route) <= corridorKm;
                    boolean dropNear = geoService.distanceToRouteKm(request.dropLat(), request.dropLng(), route) <= corridorKm;
                    double pickupProgress = geoService.progressAlongRoute(request.pickupLat(), request.pickupLng(), route);
                    double dropProgress = geoService.progressAlongRoute(request.dropLat(), request.dropLng(), route);
                    return pickupNear && dropNear && pickupProgress < dropProgress - 0.001;
                })
                .map(TripResponse::from)
                .toList();
    }

    /* ---------------- validation with explicit messages ---------------- */

    private void validateTripPayload(TripRequest request) {
        if (request.originName() == null || request.originName().isBlank()) {
            throw ApiException.badRequest("originName is required.");
        }
        if (request.destinationName() == null || request.destinationName().isBlank()) {
            throw ApiException.badRequest("destinationName is required.");
        }
        if (request.routeWaypoints() == null || request.routeWaypoints().size() < 2) {
            throw ApiException.badRequest(
                    "routeWaypoints requires at least 2 valid coordinate pairs (lng, lat) to form a LineString.");
        }
        for (int i = 0; i < request.routeWaypoints().size(); i++) {
            WaypointDto wp = request.routeWaypoints().get(i);
            if (wp == null || wp.lng() == null || wp.lat() == null
                    || !isValidLng(wp.lng()) || !isValidLat(wp.lat())) {
                throw ApiException.badRequest(
                        "Waypoint " + (i + 1) + " is invalid. Latitude must be -90 to 90 and longitude -180 to 180.");
            }
        }
        if (request.availableSeats() == null || request.availableSeats() < 0) {
            throw ApiException.badRequest("availableSeats must be 0 or more.");
        }
        if (request.availableBootKg() == null || request.availableBootKg() < 0) {
            throw ApiException.badRequest("availableBootKg must be 0 or more.");
        }
        if (request.availableSeats() + request.availableBootKg() <= 0) {
            throw ApiException.badRequest("A trip must offer at least one seat or some boot capacity.");
        }
        if (request.baseFare() == null || request.baseFare().signum() <= 0) {
            throw ApiException.badRequest("baseFare must be a positive amount.");
        }
        if (request.departureTime() == null) {
            throw ApiException.badRequest("departureTime is required.");
        }
    }

    private void validateCoordinate(Double lat, Double lng, String label) {
        if (lat == null || lng == null || !isValidLat(lat) || !isValidLng(lng)) {
            throw ApiException.badRequest(
                    label + " coordinates are invalid. Latitude must be -90 to 90 and longitude -180 to 180.");
        }
    }

    private boolean isValidLat(double lat) {
        return lat >= -90 && lat <= 90;
    }

    private boolean isValidLng(double lng) {
        return lng >= -180 && lng <= 180;
    }
}
