package com.gym.management.gym_management.service;

import com.gym.management.gym_management.exception.InvalidCredentialsException;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class TenantContext {
    private TenantContext() {
    }

    public static Long requireOrganizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof GymUserDetails details) {
            return details.getOrganizationId();
        }
        throw new InvalidCredentialsException();
    }
}
