package com.gym.management.gym_management.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PaymentWebhookRequest(
        @NotNull @Positive Long organizationId,
        @NotNull @Positive Long paymentId,
        @NotBlank @Size(max = 160) String providerReference
) {
}
