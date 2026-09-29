package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.AuditEvent;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        String actor,
        String action,
        String resourceType,
        String resourceId,
        Instant occurredAt
) {
    public static AuditEventResponse from(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getActor(),
                event.getAction(),
                event.getResourceType(),
                event.getResourceId(),
                event.getOccurredAt()
        );
    }
}
