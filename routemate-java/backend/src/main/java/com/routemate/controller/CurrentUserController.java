package com.routemate.controller;

import com.routemate.exception.ApiException;
import com.routemate.security.UserPrincipal;
import org.springframework.security.core.Authentication;

import java.util.UUID;

/** Shared helper: resolves the authenticated {@link UserPrincipal}. */
abstract class CurrentUserController {

    protected UserPrincipal currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw ApiException.forbidden("Forbidden: authentication required.");
        }
        return principal;
    }

    protected UUID currentUserId(Authentication authentication) {
        return currentUser(authentication).id();
    }
}
