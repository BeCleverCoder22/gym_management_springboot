package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.PaymentWebhookRequest;
import com.gym.management.gym_management.service.PaymentWebhookService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/webhooks")
public class PaymentWebhookController {
    private final PaymentWebhookService paymentWebhookService;

    public PaymentWebhookController(PaymentWebhookService paymentWebhookService) {
        this.paymentWebhookService = paymentWebhookService;
    }

    @PostMapping("/provider")
    public ResponseEntity<Void> providerPaymentCompleted(
            @RequestHeader("X-Payment-Signature") String signature,
            @Valid @RequestBody PaymentWebhookRequest request) {
        paymentWebhookService.processCompletedPayment(request, signature);
        return ResponseEntity.noContent().build();
    }
}
