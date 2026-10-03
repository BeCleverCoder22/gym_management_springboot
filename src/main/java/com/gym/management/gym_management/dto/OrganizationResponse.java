package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.Organization;

import java.time.Instant;

public record OrganizationResponse(Long id, String name, String slug, Instant createdAt) {
    public static OrganizationResponse from(Organization organization) {
        return new OrganizationResponse(
                organization.getId(), organization.getName(),
                organization.getSlug(), organization.getCreatedAt());
    }
}
