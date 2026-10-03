package com.gym.management.gym_management.service;

import com.gym.management.gym_management.dto.OrganizationResponse;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganizationService {
    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrentOrganization() {
        return organizationRepository.findByIdAndActiveTrue(TenantContext.requireOrganizationId())
                .map(OrganizationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable."));
    }
}
