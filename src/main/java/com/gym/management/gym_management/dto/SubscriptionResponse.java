package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionResponse(
        Long id,
        Long customerId,
        String customerName,
        Long packId,
        String packName,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal monthlyPrice,
        SubscriptionStatus status
) {
    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getCustomer().getId(),
                subscription.getCustomer().getLastName() + " " + subscription.getCustomer().getFirstName(),
                subscription.getPack().getId(),
                subscription.getOfferNameAtPurchase(),
                subscription.getStartDate(),
                subscription.getEndDate(),
                subscription.getMonthlyPriceAtPurchase(),
                subscription.getStatus()
        );
    }
}
