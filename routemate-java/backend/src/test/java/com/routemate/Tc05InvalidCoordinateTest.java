package com.routemate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-05 — Driver (boundary):
 * Publishing with invalid coordinates (e.g. lat 453) must be rejected with a
 * clear latitude-range message. The React frontend additionally blocks the
 * submission BEFORE any fetch (see frontend/src/api.js validators), so invalid
 * data never leaves the browser.
 */
class Tc05InvalidCoordinateTest extends ApiTestBase {

    @Test
    void publishWithLatitude453IsRejectedWithRangeMessage() {
        String email = uniqueEmail("qa.driver");
        String token = registerAndLogin("QA Driver", email, "QaDriver@123", "DRIVER");
        String driverId = toJson(rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", "QaDriver@123"), String.class))
                .get("userId").asText();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("driverId", driverId);
        body.put("originName", "Pune");
        body.put("destinationName", "Mumbai");
        body.put("routeWaypoints", List.of(
                Map.of("lng", 73.8567, "lat", 453),
                Map.of("lng", 72.8777, "lat", 19.0760)));
        body.put("availableSeats", 3);
        body.put("availableBootKg", 15);
        body.put("baseFare", 350);
        body.put("departureTime", "2030-01-01T10:00:00Z");

        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/trips", new HttpEntity<>(body, authHeaders(token)), String.class);

        assertEquals(400, response.getStatusCode().value(),
                "invalid latitude must be rejected, got: " + response.getStatusCode());
        assertTrue(response.getBody().contains("-90") && response.getBody().contains("90"),
                "error must explain the latitude range, got: " + response.getBody());
    }

    @Test
    void publishWithSingleWaypointIsRejected() {
        String email = uniqueEmail("qa.driver");
        String token = registerAndLogin("QA Driver", email, "QaDriver@123", "DRIVER");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("originName", "Pune");
        body.put("destinationName", "Mumbai");
        body.put("routeWaypoints", List.of(Map.of("lng", 73.8567, "lat", 18.5204)));
        body.put("availableSeats", 3);
        body.put("availableBootKg", 15);
        body.put("baseFare", 350);
        body.put("departureTime", "2030-01-01T10:00:00Z");

        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/trips", new HttpEntity<>(body, authHeaders(token)), String.class);

        assertEquals(400, response.getStatusCode().value());
        assertTrue(response.getBody().contains("at least 2"),
                "error must mention the 2-point LineString rule, got: " + response.getBody());
    }
}
