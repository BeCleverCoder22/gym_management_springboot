package com.gym.management.gym_management.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;

import java.math.BigDecimal;

public record PackRequest(
        @NotBlank @Size(max = 100) String offerName,
        @Size(max = 1000) String description,
        @Positive @Max(120) int durationMonths,
        @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2) BigDecimal monthlyPrice
) {
}
