package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-04 — Driver / spatial:
 * Publish a trip with a valid 2-point LineString (Pune → Mumbai).
 * Expect 200 with a generated tripId; the trip must appear in the driver feed.
 */
class Tc04PublishTripTest extends ApiTestBase {

    @Test
    void publishPuneMumbaiTripWithValidLineString() {
        String email = uniqueEmail("qa.driver");
        String[] auth = registerAndLogin2("QA Driver", email, "QaDriver@123", "DRIVER");
        String token = auth[0];
        String driverId = auth[1];

        ResponseEntity<String> response = publishTrip(token, driverId, 350);

        assertEquals(200, response.getStatusCode().value(), "publish failed: " + response.getBody());
        JsonNode json = toJson(response);
        assertNotNull(json.get("tripId").asText(), "tripId missing in response");
        assertEquals("QA Driver", json.get("driverName").asText());
        assertEquals("Pune", json.get("originName").asText());
        assertEquals("Mumbai", json.get("destinationName").asText());
        assertEquals(3, json.get("availableSeats").asInt());
        assertEquals(15.0, json.get("availableBootKg").asDouble(), 0.001);
        assertEquals(350.0, json.get("baseFare").asDouble(), 0.001);
        assertEquals("SCHEDULED", json.get("status").asText());
        String tripId = json.get("tripId").asText();

        // The trip must render in the UI list == present in the driver feed.
        ResponseEntity<String> feed = rest.exchange(
                "/api/v1/trips/driver/" + driverId, org.springframework.http.HttpMethod.GET,
                new org.springframework.http.HttpEntity<>(authHeaders(token)), String.class);
        assertEquals(200, feed.getStatusCode().value());
        JsonNode trips = toJson(feed).get("trips");
        assertTrue(trips.isArray() && trips.size() >= 1, "driver feed is empty");
        boolean found = false;
        for (JsonNode t : trips) {
            if (tripId.equals(t.get("tripId").asText())) found = true;
        }
        assertTrue(found, "published trip " + tripId + " missing from driver feed");
    }

    /** register + login returning {token, userId}. */
    private String[] registerAndLogin2(String name, String email, String password, String role) {
        String token = registerAndLogin(name, email, password, role);
        JsonNode login = toJson(rest.postForEntity("/api/v1/auth/login",
                java.util.Map.of("email", email, "password", password), String.class));
        return new String[]{token, login.get("userId").asText()};
    }
}
