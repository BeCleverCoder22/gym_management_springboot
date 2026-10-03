package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.entity.NotificationStatus;
import com.gym.management.gym_management.repository.NotificationOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class NotificationProcessingService {
    private final NotificationOutboxRepository outboxRepository;

    public NotificationProcessingService(NotificationOutboxRepository outboxRepository) {
        this.outboxRepository = outboxRepository;
    }

    @Transactional
    public NotificationOutbox claim(Long id) {
        NotificationOutbox item = outboxRepository.findByIdForUpdate(id).orElse(null);
        if (item == null || item.getNextAttemptAt().isAfter(Instant.now())) {
            return null;
        }
        if (item.getStatus() != NotificationStatus.QUEUED
                && item.getStatus() != NotificationStatus.RETRY
                && item.getStatus() != NotificationStatus.PROCESSING) {
            return null;
        }
        item.markProcessing();
        return outboxRepository.save(item);
    }

    @Transactional
    public void markSent(Long id) {
        outboxRepository.findByIdForUpdate(id).ifPresent(item -> {
            item.markSent();
            outboxRepository.save(item);
        });
    }

    @Transactional
    public void markFailed(Long id, String safeErrorCode) {
        outboxRepository.findByIdForUpdate(id).ifPresent(item -> {
            item.markFailed(safeErrorCode);
            outboxRepository.save(item);
        });
    }
}
