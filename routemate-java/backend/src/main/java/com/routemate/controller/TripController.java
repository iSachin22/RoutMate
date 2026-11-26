package com.routemate.controller;

import com.routemate.dto.TripRequest;
import com.routemate.dto.TripResponse;
import com.routemate.dto.TripSearchRequest;
import com.routemate.service.TripService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Trip endpoints: publish (driver), driver feed, corridor search. */
@RestController
@RequestMapping("/api/v1/trips")
public class TripController extends CurrentUserController {

    private final TripService tripService;

    public TripController(TripService tripService) {
        this.tripService = tripService;
    }

    /** POST /api/v1/trips — publish a trip (DRIVER only). */
    @PostMapping
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<TripResponse> publishTrip(@RequestBody TripRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(tripService.publishTrip(request, currentUserId(authentication)));
    }

    /** POST /api/v1/trips/search — corridor search (any authenticated user). */
    @PostMapping("/search")
    public ResponseEntity<Map<String, List<TripResponse>>> search(@RequestBody TripSearchRequest request) {
        return ResponseEntity.ok(Map.of("trips", tripService.search(request)));
    }

    /** GET /api/v1/trips/driver/{driverId} — the driver's trips (DRIVER, own feed only). */
    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Map<String, List<TripResponse>>> driverTrips(@PathVariable UUID driverId,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(Map.of("trips",
                tripService.getDriverTrips(driverId, currentUserId(authentication))));
    }
}
