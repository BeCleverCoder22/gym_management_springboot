package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.dto.NotificationOutboxResponse;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.service.NotificationOutboxAdminService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Set;

@RestController
@RequestMapping("/api/notifications/outbox")
public class NotificationController {
    private final NotificationOutboxAdminService notificationService;

    public NotificationController(NotificationOutboxAdminService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PageResponse<NotificationOutboxResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return PageResponse.from(
                notificationService.list(PaginationSupport.create(
                        page, size, sort, Set.of("id", "eventType", "status", "createdAt"))),
                NotificationOutboxResponse::from);
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(@PathVariable Long id) {
        notificationService.retry(id);
        return ResponseEntity.noContent().build();
    }
}
