package com.routemate.dto;

import com.routemate.domain.Role;

import java.util.UUID;

/** POST /api/v1/auth/login — success payload exactly as in the contract. */
public record LoginResponse(String token, UUID userId, Role role, String name) {
}
