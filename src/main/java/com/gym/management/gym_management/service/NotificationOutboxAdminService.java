package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.NotificationOutboxRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxAdminService {
    private final NotificationOutboxRepository outboxRepository;
    private final AuditService auditService;

    public NotificationOutboxAdminService(
            NotificationOutboxRepository outboxRepository, AuditService auditService) {
        this.outboxRepository = outboxRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public Page<NotificationOutbox> list(Pageable pageable) {
        return outboxRepository.findByOrganization_Id(TenantContext.requireOrganizationId(), pageable);
    }

    @Transactional
    public void retry(Long id) {
        NotificationOutbox notification = outboxRepository.findByIdAndOrganizationForUpdate(
                        id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification introuvable."));
        try {
            notification.retryNow();
        } catch (IllegalStateException exception) {
            throw new ConflictException("Cette notification ne peut pas être relancée.");
        }
        outboxRepository.save(notification);
        auditService.record("NOTIFICATION_RETRY", "NOTIFICATION", id);
    }
}
