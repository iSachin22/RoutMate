package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-07 — Passenger (parcel):
 * Book a 5 kg parcel. Expect bookedSeats 0, an OTP, and the trip's boot
 * capacity reduced (15 kg → 10 kg).
 */
class Tc07BookParcelTest extends ApiTestBase {

    @Test
    void bookParcelReducesBootWeightAndGeneratesOtp() {
        String driverEmail = uniqueEmail("qa.driver");
        String driverToken = registerAndLogin("QA Driver", driverEmail, "QaDriver@123", "DRIVER");
        String driverId = userIdAfterLogin(driverEmail, "QaDriver@123");
        JsonNode trip = toJson(publishTrip(driverToken, driverId, 350));
        String tripId = trip.get("tripId").asText();

        String paxEmail = uniqueEmail("qa.pax");
        String paxToken = registerAndLogin("QA Passenger", paxEmail, "QaPassenger@123", "PASSENGER");
        String paxId = userIdAfterLogin(paxEmail, "QaPassenger@123");

        ResponseEntity<String> response = book(paxToken, tripId, paxId, 0, 5.0, "PARCEL");

        assertEquals(200, response.getStatusCode().value(), "parcel booking failed: " + response.getBody());
        JsonNode json = toJson(response);
        assertNotNull(json.get("bookingId"));
        assertEquals(0, json.get("bookedSeats").asInt(), "PARCEL bookings must have bookedSeats 0");
        assertEquals(5.0, json.get("parcelWeightKg").asDouble(), 0.001);
        String otp = json.get("otpCode").asText();
        assertTrue(otp.matches("\\d{4}"), "otpCode must be 4 digits, got: " + otp);
        // parcel fare = baseFare + 15/kg
        assertEquals(350.0 + 5 * 15, json.get("calculatedFare").asDouble(), 0.001);

        // boot weight must have been consumed on the trip
        ResponseEntity<String> feed = rest.exchange(
                "/api/v1/trips/driver/" + driverId, org.springframework.http.HttpMethod.GET,
                new HttpEntity<>(authHeaders(driverToken)), String.class);
        JsonNode trips = toJson(feed).get("trips");
        JsonNode updated = null;
        for (JsonNode t : trips) {
            if (tripId.equals(t.get("tripId").asText())) updated = t;
        }
        assertNotNull(updated, "trip missing from driver feed after booking");
        assertEquals(10.0, updated.get("availableBootKg").asDouble(), 0.001,
                "boot weight must reduce from 15 kg to 10 kg after a 5 kg parcel");
    }

    private String userIdAfterLogin(String email, String password) {
        return toJson(rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class))
                .get("userId").asText();
    }
}
