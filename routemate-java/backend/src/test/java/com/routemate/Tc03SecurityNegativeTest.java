package com.routemate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-03 — Security (negative):
 * POST /api/v1/trips without a token or with a PASSENGER token must return
 * HTTP 403 Forbidden (the UI maps this to the clear "Unauthorized / Role
 * Mismatch" alert — never a generic "Network Error").
 */
class Tc03SecurityNegativeTest extends ApiTestBase {

    @Test
    void publishWithoutTokenReturns403() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = rest.postForEntity("/api/v1/trips",
                new HttpEntity<>(Map.of("originName", "X"), headers), String.class);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("Forbidden"),
                "expected a Forbidden message, got: " + response.getBody());
    }

    @Test
    void publishWithPassengerTokenReturns403() {
        String email = uniqueEmail("qa.pax");
        String token = registerAndLogin("QA Passenger", email, "QaPassenger@123", "PASSENGER");

        ResponseEntity<String> response = rest.postForEntity("/api/v1/trips",
                new HttpEntity<>(Map.of("originName", "X", "destinationName", "Y"), authHeaders(token)),
                String.class);
        assertEquals(403, response.getStatusCode().value());
        assertTrue(response.getBody() != null && response.getBody().contains("Forbidden"),
                "expected a Forbidden message, got: " + response.getBody());
    }
}
