package com.routemate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-01 — Security / CORS:
 * Preflight OPTIONS request from 127.0.0.1:5500 to localhost:8081 must succeed
 * with CORS headers and no CORS block.
 */
class Tc01CorsPreflightTest extends ApiTestBase {

    @Test
    void preflightOptionsFromLiveServerOriginIsAllowed() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Origin", "http://127.0.0.1:5500");
        headers.set("Access-Control-Request-Method", "POST");
        headers.set("Access-Control-Request-Headers", "authorization,content-type");

        ResponseEntity<String> response = rest.exchange(
                "/api/v1/trips", HttpMethod.OPTIONS, new HttpEntity<>(headers), String.class);

        int status = response.getStatusCode().value();
        assertTrue(status == 200 || status == 204,
                "OPTIONS preflight must answer 200/204, got " + status);
        String allowOrigin = response.getHeaders().getFirst("Access-Control-Allow-Origin");
        assertNotNull(allowOrigin, "Access-Control-Allow-Origin header missing on preflight");
        assertEquals("http://127.0.0.1:5500", allowOrigin);
        String allowHeaders = response.getHeaders().getFirst("Access-Control-Allow-Headers");
        assertNotNull(allowHeaders, "Access-Control-Allow-Headers missing on preflight");
        assertTrue(allowHeaders.toLowerCase().contains("authorization"),
                "Preflight must allow the Authorization header");
    }
}
