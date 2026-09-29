package com.gym.management.gym_management.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminUserUpdateRequest(
        @Size(min = 3, max = 50) String username,
        @Email @Size(max = 254) String email,
        @Pattern(regexp = "USER|ADMIN") String role
) {
}
