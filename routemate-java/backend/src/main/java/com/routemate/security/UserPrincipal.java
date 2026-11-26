package com.routemate.security;

import com.routemate.domain.Role;

import java.util.UUID;

/**
 * The authenticated principal attached to the Spring Security context.
 */
public record UserPrincipal(UUID id, String name, Role role) {
}
