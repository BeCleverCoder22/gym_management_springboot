package com.gym.management.gym_management.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record SubscriptionRequest(
        @NotNull @Positive Long customerId,
        @NotNull @Positive Long packId,
        @NotNull LocalDate startDate
) {
}
