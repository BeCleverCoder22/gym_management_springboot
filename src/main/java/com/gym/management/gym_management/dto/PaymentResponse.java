package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.Payment;
import com.gym.management.gym_management.entity.PaymentMethod;
import com.gym.management.gym_management.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        Long subscriptionId,
        BigDecimal amount,
        BigDecimal refundedAmount,
        String currency,
        PaymentMethod method,
        PaymentStatus status,
        String providerReference,
        Instant createdAt,
        Instant settledAt
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(), payment.getSubscription().getId(),
                payment.getAmount(), payment.getRefundedAmount(), payment.getCurrency(),
                payment.getMethod(), payment.getStatus(), payment.getProviderReference(),
                payment.getCreatedAt(), payment.getSettledAt());
    }
}
