package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.SubscriptionRequest;
import com.gym.management.gym_management.dto.SubscriptionResponse;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/subscriptions")
@Tag(name = "Abonnements")
@SecurityRequirement(name = "bearerAuth")
public class SubscriptionController {
    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping
    public PageResponse<SubscriptionResponse> getAllSubscriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort) {
        Pageable pageable = subscriptionPage(page, size, sort);
        return PageResponse.from(
                subscriptionService.getAllSubscriptions(pageable), SubscriptionResponse::from);
    }

    @GetMapping("/{id}")
    public SubscriptionResponse getSubscriptionById(@PathVariable Long id) {
        return SubscriptionResponse.from(subscriptionService.getSubscriptionById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un abonnement et figer les conditions de l'offre")
    public ResponseEntity<SubscriptionResponse> addSubscription(
            @Valid @RequestBody SubscriptionRequest request) {
        SubscriptionResponse response = SubscriptionResponse.from(subscriptionService.addSubscription(
                request.customerId(), request.packId(), request.startDate()));
        return ResponseEntity.created(URI.create("/api/subscriptions/" + response.id())).body(response);
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "Renouveler un abonnement en créant une nouvelle période")
    public ResponseEntity<SubscriptionResponse> renewSubscription(@PathVariable Long id) {
        SubscriptionResponse response =
                SubscriptionResponse.from(subscriptionService.renewSubscription(id));
        return ResponseEntity.created(URI.create("/api/subscriptions/" + response.id())).body(response);
    }

    @GetMapping("/customer/{customerId}")
    public PageResponse<SubscriptionResponse> getSubscriptionsByCustomer(
            @PathVariable Long customerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "startDate,desc") String sort) {
        return PageResponse.from(
                subscriptionService.getSubscriptionsByCustomerId(
                        customerId, subscriptionPage(page, size, sort)),
                SubscriptionResponse::from);
    }

    @PutMapping("/{id}")
    public SubscriptionResponse updateSubscription(
            @PathVariable Long id, @Valid @RequestBody SubscriptionRequest request) {
        return SubscriptionResponse.from(subscriptionService.updateSubscription(
                id, request.customerId(), request.packId(), request.startDate()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Annuler et archiver un abonnement")
    public ResponseEntity<Void> cancelSubscription(@PathVariable Long id) {
        subscriptionService.deleteSubscription(id);
        return ResponseEntity.noContent().build();
    }

    private Pageable subscriptionPage(int page, int size, String sort) {
        return PaginationSupport.create(
                page, size, sort, Set.of("id", "startDate", "endDate", "status"));
    }
}
