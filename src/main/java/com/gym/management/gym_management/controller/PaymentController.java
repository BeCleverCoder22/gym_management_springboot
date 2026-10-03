package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.dto.*;
import com.gym.management.gym_management.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Set;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public PageResponse<PaymentResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "amount", "status", "createdAt"));
        return PageResponse.from(paymentService.list(pageable), PaymentResponse::from);
    }

    @PostMapping
    @Operation(summary = "Créer un paiement idempotent; les méthodes électroniques restent en attente")
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentRequest request) {
        PaymentResponse response = PaymentResponse.from(paymentService.create(
                request.subscriptionId(), request.amount(), request.currency(),
                request.method(), request.idempotencyKey()));
        return ResponseEntity.created(URI.create("/api/payments/" + response.id())).body(response);
    }

    @PostMapping("/{id}/cash-confirmation")
    public PaymentResponse confirmCash(@PathVariable Long id) {
        return PaymentResponse.from(paymentService.completeCashPayment(id));
    }

    @PostMapping("/{id}/refunds")
    public ResponseEntity<RefundResponse> requestRefund(
            @PathVariable Long id, @Valid @RequestBody RefundRequest request) {
        RefundResponse response = RefundResponse.from(
                paymentService.requestRefund(id, request.amount(), request.reason()));
        return ResponseEntity.created(URI.create("/api/payments/refunds/" + response.id())).body(response);
    }

    @PostMapping("/refunds/{refundId}/complete")
    public RefundResponse completeRefund(@PathVariable Long refundId) {
        return RefundResponse.from(paymentService.completeRefund(refundId));
    }
}
