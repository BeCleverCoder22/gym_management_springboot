package com.gym.management.gym_management.service;

import com.gym.management.gym_management.dto.PaymentWebhookRequest;
import com.gym.management.gym_management.exception.InvalidCredentialsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Service
public class PaymentWebhookService {
    private final PaymentService paymentService;
    private final String secret;

    public PaymentWebhookService(
            PaymentService paymentService,
            @Value("${app.payments.webhook-secret:}") String secret) {
        this.paymentService = paymentService;
        this.secret = secret;
        if (!secret.isBlank() && secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("Payment webhook secret must contain at least 32 UTF-8 bytes.");
        }
    }

    public void processCompletedPayment(PaymentWebhookRequest request, String signature) {
        if (secret.isBlank()) {
            throw new IllegalStateException("Payment webhook integration is not configured.");
        }
        byte[] expectedSignature = sign(canonicalPayload(request));
        byte[] receivedSignature;
        try {
            receivedSignature = HexFormat.of().parseHex(signature);
        } catch (IllegalArgumentException exception) {
            throw new InvalidCredentialsException();
        }
        if (!MessageDigest.isEqual(expectedSignature, receivedSignature)) {
            throw new InvalidCredentialsException();
        }
        paymentService.completeProviderPayment(
                request.organizationId(), request.paymentId(), request.providerReference());
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (java.security.GeneralSecurityException exception) {
            throw new IllegalStateException("Cannot verify payment webhook signature.", exception);
        }
    }

    private String canonicalPayload(PaymentWebhookRequest request) {
        return request.organizationId() + ":" + request.paymentId() + ":" + request.providerReference();
    }
}
