package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.AuditEvent;
import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.repository.AuditEventRepository;
import com.gym.management.gym_management.repository.OrganizationRepository;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;
    private final OrganizationRepository organizationRepository;

    public AuditService(
            AuditEventRepository auditEventRepository,
            OrganizationRepository organizationRepository) {
        this.auditEventRepository = auditEventRepository;
        this.organizationRepository = organizationRepository;
    }

    public void record(String action, String resourceType, Object resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                ? "anonymous"
                : authentication.getName();
        recordOrganization(TenantContext.requireOrganizationId(), action, resourceType, resourceId, actor);
    }

    public void recordSystem(String action, String resourceType, Object resourceId) {
        recordSystem(TenantContext.requireOrganizationId(), action, resourceType, resourceId);
    }

    public void recordSystem(Long organizationId, String action, String resourceType, Object resourceId) {
        recordOrganization(organizationId, action, resourceType, resourceId, "system");
    }

    public void recordOrganization(Long organizationId, String action, String resourceType, Object resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                ? "anonymous"
                : authentication.getName();
        recordOrganization(organizationId, action, resourceType, resourceId, actor);
    }

    private void recordOrganization(
            Long organizationId, String action, String resourceType, Object resourceId, String actor) {
        Organization organization = organizationRepository.getReferenceById(organizationId);
        auditEventRepository.save(new AuditEvent(
                organization, actor, action, resourceType, String.valueOf(resourceId)));
    }

    public Page<AuditEvent> findEvents(Pageable pageable) {
        return auditEventRepository.findByOrganization_Id(
                TenantContext.requireOrganizationId(), pageable);
    }
}
