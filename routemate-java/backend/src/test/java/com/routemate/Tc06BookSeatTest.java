package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-06 — Passenger:
 * Book a seat on an active trip. Expect 200 with bookingId, calculatedFare and
 * a 4-digit otpCode.
 */
class Tc06BookSeatTest extends ApiTestBase {

    @Test
    void bookSeatOnActiveTripReturnsFareAndOtp() {
        // driver publishes
        String driverEmail = uniqueEmail("qa.driver");
        String driverToken = registerAndLogin("QA Driver", driverEmail, "QaDriver@123", "DRIVER");
        String driverId = userIdAfterLogin(driverEmail, "QaDriver@123");
        JsonNode trip = toJson(publishTrip(driverToken, driverId, 350));
        String tripId = trip.get("tripId").asText();

        // passenger books a seat
        String paxEmail = uniqueEmail("qa.pax");
        String paxToken = registerAndLogin("QA Passenger", paxEmail, "QaPassenger@123", "PASSENGER");
        String paxId = userIdAfterLogin(paxEmail, "QaPassenger@123");

        ResponseEntity<String> response = bookSeat(paxToken, tripId, paxId);

        assertEquals(200, response.getStatusCode().value(), "booking failed: " + response.getBody());
        JsonNode json = toJson(response);
        assertNotNull(json.get("bookingId").asText());
        assertEquals(1, json.get("bookedSeats").asInt());
        assertEquals("SEAT", json.get("bookingType").asText());
        assertEquals(350.0, json.get("calculatedFare").asDouble(), 0.001,
                "SEAT fare must be baseFare × bookedSeats");
        String otp = json.get("otpCode").asText();
        assertTrue(otp.matches("\\d{4}"), "otpCode must be 4 digits, got: " + otp);
        assertEquals("CONFIRMED", json.get("status").asText());
    }

    private String userIdAfterLogin(String email, String password) {
        return toJson(rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class))
                .get("userId").asText();
    }
}
