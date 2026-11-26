package com.routemate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.ActiveProfiles;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Shared plumbing for the RouteMate QA test suite (TC-01 … TC-08).
 * Runs against a real HTTP stack on a random port with an in-memory H2 database,
 * so the whole security filter chain + CORS + JSON stack is exercised.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class ApiTestBase {

    @Autowired
    protected TestRestTemplate rest;

    protected final ObjectMapper mapper = new ObjectMapper();

    /**
     * Uses Java's built-in HttpClient under the hood: it forwards the custom
     * CORS preflight headers (Origin, Access-Control-Request-*) that the legacy
     * HttpURLConnection silently strips, and it handles 401 responses cleanly.
     */
    @PostConstruct
    void useJdkHttpClient() {
        rest.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    /** Pune → Mumbai (exactly the blueprint example). */
    protected static final List<Map<String, Object>> PUNE_MUMBAI_WAYPOINTS = List.of(
            Map.of("lng", 73.8567, "lat", 18.5204),
            Map.of("lng", 72.8777, "lat", 19.0760));

    protected String uniqueEmail(String prefix) {
        return prefix + "." + UUID.randomUUID().toString().substring(0, 8) + "@routemate.test";
    }

    protected JsonNode toJson(ResponseEntity<String> response) {
        try {
            return mapper.readTree(response.getBody() == null ? "{}" : response.getBody());
        } catch (Exception ex) {
            throw new AssertionError("Response is not valid JSON: " + response.getBody(), ex);
        }
    }

    protected ResponseEntity<String> register(String name, String email, String password, String role) {
        Map<String, Object> body = Map.of("name", name, "email", email, "password", password, "role", role);
        return rest.postForEntity("/api/v1/auth/register", body, String.class);
    }

    /** Registers + logs in, asserts 200, and returns the JWT. */
    protected String registerAndLogin(String name, String email, String password, String role) {
        ResponseEntity<String> reg = register(name, email, password, role);
        Assertions.assertTrue(reg.getStatusCode().is2xxSuccessful(),
                "register failed: " + reg.getStatusCode() + " " + reg.getBody());
        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class);
        Assertions.assertEquals(200, login.getStatusCode().value(), "login failed: " + login.getBody());
        return toJson(login).get("token").asText();
    }

    protected HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        return headers;
    }

    protected ResponseEntity<String> publishTrip(String token, String driverId, int baseFare) {
        return publishTrip(token, driverId, PUNE_MUMBAI_WAYPOINTS, 3, 15.0, baseFare);
    }

    protected ResponseEntity<String> publishTrip(String token, String driverId,
                                                 List<Map<String, Object>> waypoints,
                                                 int seats, double bootKg, int baseFare) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("driverId", driverId);
        body.put("originName", "Pune");
        body.put("destinationName", "Mumbai");
        body.put("routeWaypoints", waypoints);
        body.put("availableSeats", seats);
        body.put("availableBootKg", bootKg);
        body.put("baseFare", baseFare);
        body.put("departureTime", "2030-01-01T10:00:00Z");
        return rest.postForEntity("/api/v1/trips", new HttpEntity<>(body, authHeaders(token)), String.class);
    }

    protected ResponseEntity<String> bookSeat(String token, String tripId, String passengerId) {
        return book(token, tripId, passengerId, 1, 0, "SEAT");
    }

    protected ResponseEntity<String> book(String token, String tripId, String passengerId,
                                          int bookedSeats, double parcelWeightKg, String bookingType) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("tripId", tripId);
        body.put("passengerId", passengerId);
        body.put("bookedSeats", bookedSeats);
        body.put("parcelWeightKg", parcelWeightKg);
        body.put("pickupLat", 19.0948);
        body.put("pickupLng", 74.7480);
        body.put("dropLat", 18.5204);
        body.put("dropLng", 73.8567);
        body.put("bookingType", bookingType);
        return rest.postForEntity("/api/v1/bookings", new HttpEntity<>(body, authHeaders(token)), String.class);
    }

    /** Logs in a user and returns [token, userId] from the login response. */
    protected String[] login(String email, String password) {
        ResponseEntity<String> login = rest.postForEntity("/api/v1/auth/login",
                Map.of("email", email, "password", password), String.class);
        Assertions.assertEquals(200, login.getStatusCode().value(), "login failed: " + login.getBody());
        JsonNode json = toJson(login);
        return new String[]{json.get("token").asText(), json.get("userId").asText()};
    }
}
