package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.entity.NotificationStatus;

import java.time.Instant;

public record NotificationOutboxResponse(
        Long id,
        String eventType,
        NotificationStatus status,
        int attempts,
        Instant createdAt,
        Instant nextAttemptAt,
        Instant sentAt,
        String lastError
) {
    public static NotificationOutboxResponse from(NotificationOutbox notification) {
        return new NotificationOutboxResponse(
                notification.getId(), notification.getEventType(), notification.getStatus(),
                notification.getAttempts(), notification.getCreatedAt(),
                notification.getNextAttemptAt(), notification.getSentAt(), notification.getLastError());
    }
}
