package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.PaymentRefund;
import com.gym.management.gym_management.entity.RefundStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record RefundResponse(
        Long id,
        Long paymentId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        Instant createdAt,
        Instant completedAt
) {
    public static RefundResponse from(PaymentRefund refund) {
        return new RefundResponse(
                refund.getId(), refund.getPayment().getId(), refund.getAmount(),
                refund.getReason(), refund.getStatus(), refund.getCreatedAt(), refund.getCompletedAt());
    }
}
