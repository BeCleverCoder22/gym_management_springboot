package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.AuditEvent;
import com.gym.management.gym_management.repository.AuditEventRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
public class AuditService {
    private final AuditEventRepository auditEventRepository;

    public AuditService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    public void record(String action, String resourceType, Object resourceId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String actor = authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                ? "anonymous"
                : authentication.getName();
        auditEventRepository.save(new AuditEvent(
                actor, action, resourceType, String.valueOf(resourceId)));
    }

    public void recordSystem(String action, String resourceType, Object resourceId) {
        auditEventRepository.save(new AuditEvent(
                "system", action, resourceType, String.valueOf(resourceId)));
    }

    public Page<AuditEvent> findEvents(Pageable pageable) {
        return auditEventRepository.findAll(pageable);
    }
}
