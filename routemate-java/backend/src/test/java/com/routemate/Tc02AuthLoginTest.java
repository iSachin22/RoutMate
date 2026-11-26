package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * TC-02 — Auth:
 * Login with valid Driver credentials returns 200 with a JWT and user profile.
 */
class Tc02AuthLoginTest extends ApiTestBase {

    @Test
    void loginWithValidDriverCredentialsReturnsJwt() {
        String email = uniqueEmail("qa.driver");
        String password = "QaDriver@123";
        ResponseEntity<String> reg = register("QA Driver", email, password, "DRIVER");
        assertTrue(reg.getStatusCode().is2xxSuccessful(), "register failed: " + reg.getBody());

        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                java.util.Map.of("email", email, "password", password), String.class);

        assertEquals(200, login.getStatusCode().value());
        JsonNode json = toJson(login);
        String token = json.get("token").asText();
        assertNotNull(token);
        assertEquals(3, token.split("\\.").length, "token must look like a JWT");
        assertEquals("DRIVER", json.get("role").asText());
        assertNotNull(json.get("userId"));
        assertTrue(json.get("userId").asText().length() > 0);
        assertEquals("QA Driver", json.get("name").asText());
    }

    @Test
    void loginWithWrongPasswordIsRejected() {
        String email = uniqueEmail("qa.driver");
        register("QA Driver", email, "QaDriver@123", "DRIVER");
        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                java.util.Map.of("email", email, "password", "wrong-password"), String.class);
        assertEquals(401, login.getStatusCode().value());
    }
}
