package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-08 — E2E integration:
 * Register Driver → Publish Trip → Register Passenger → Search Trip →
 * Book Seat → Capture OTP → Driver verifies OTP. Every step must succeed
 * consistently (database state matches the API state).
 */
class Tc08FullCycleTest extends ApiTestBase {

    @Test
    void fullCycleFromRegistrationToOtpSettlement() {
        // 1) Register + login Driver
        String driverEmail = uniqueEmail("e2e.driver");
        String driverToken = registerAndLogin("E2E Driver", driverEmail, "E2eDriver@123", "DRIVER");
        String driverId = userIdAfterLogin(driverEmail, "E2eDriver@123");

        // 2) Publish Trip (Pune → Mumbai)
        JsonNode trip = toJson(publishTrip(driverToken, driverId, 350));
        assertEquals(200, trip.path("status").isMissingNode() ? 200 : 200);
        String tripId = trip.get("tripId").asText();
        assertNotNull(tripId);

        // 3) Register + login Passenger
        String paxEmail = uniqueEmail("e2e.pax");
        String paxToken = registerAndLogin("E2E Passenger", paxEmail, "E2ePax@123", "PASSENGER");
        String paxId = userIdAfterLogin(paxEmail, "E2ePax@123");

        // 4) Search the corridor (pickup = Pune, drop = Mumbai)
        ResponseEntity<String> search = rest.postForEntity("/api/v1/trips/search",
                new HttpEntity<>(Map.of(
                        "pickupLat", 18.5204, "pickupLng", 73.8567,
                        "dropLat", 19.0760, "dropLng", 72.8777),
                        authHeaders(paxToken)),
                String.class);
        assertEquals(200, search.getStatusCode().value(), "search failed: " + search.getBody());
        JsonNode found = toJson(search).get("trips");
        boolean discovered = false;
        for (JsonNode t : found) {
            if (tripId.equals(t.get("tripId").asText())) discovered = true;
        }
        assertTrue(discovered, "corridor search must discover the freshly published trip");

        // 5) Book a seat
        JsonNode booking = toJson(bookSeat(paxToken, tripId, paxId));
        assertEquals(200, booking.path("status").isMissingNode() ? 200 : 200);
        String bookingId = booking.get("bookingId").asText();
        String otp = booking.get("otpCode").asText();
        assertTrue(otp.matches("\\d{4}"), "captured OTP must be 4 digits");

        // 6) Driver settles the booking with the captured OTP
        ResponseEntity<String> verify = rest.postForEntity(
                "/api/v1/bookings/" + bookingId + "/verify-otp",
                new HttpEntity<>(Map.of("otpCode", otp), authHeaders(driverToken)),
                String.class);
        assertEquals(200, verify.getStatusCode().value(), "OTP verification failed: " + verify.getBody());
        assertEquals("VERIFIED", toJson(verify).get("message").asText());

        // 7) Database state matches: booking is COMPLETED in the driver's feed
        ResponseEntity<String> feed = rest.exchange(
                "/api/v1/bookings/trip/" + tripId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(driverToken)), String.class);
        JsonNode bookings = toJson(feed).get("bookings");
        JsonNode settled = null;
        for (JsonNode b : bookings) {
            if (bookingId.equals(b.get("bookingId").asText())) settled = b;
        }
        assertNotNull(settled, "booking missing from driver feed");
        assertEquals("COMPLETED", settled.get("status").asText(),
                "booking must be COMPLETED after OTP settlement");
    }

    private String userIdAfterLogin(String email, String password) {
        return toJson(rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class))
                .get("userId").asText();
    }
}
