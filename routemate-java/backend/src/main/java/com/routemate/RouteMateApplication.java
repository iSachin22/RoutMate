package com.routemate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * RouteMate — carpooling &amp; parcel delivery platform.
 * Spring Boot 3.3.5 · Java 17 · stateless JWT security · MySQL.
 */
@SpringBootApplication
public class RouteMateApplication {

    public static void main(String[] args) {
        SpringApplication.run(RouteMateApplication.class, args);
    }
}
