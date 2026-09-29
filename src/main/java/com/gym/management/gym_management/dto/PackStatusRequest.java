package com.gym.management.gym_management.dto;

import jakarta.validation.constraints.NotNull;

public record PackStatusRequest(@NotNull Boolean active) {
}
