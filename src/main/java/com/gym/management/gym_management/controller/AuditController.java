package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.dto.AuditEventResponse;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.service.AuditService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/audit")
@Tag(name = "Audit")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {
    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping
    public PageResponse<AuditEventResponse> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "actor", "action", "occurredAt"));
        return PageResponse.from(auditService.findEvents(pageable), AuditEventResponse::from);
    }
}
