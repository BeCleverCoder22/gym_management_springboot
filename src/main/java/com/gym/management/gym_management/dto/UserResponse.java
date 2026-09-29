package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime lastLogin
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().name(),
                !Boolean.FALSE.equals(user.getEnabled()),
                user.getCreatedAt(),
                user.getLastLogin()
        );
    }
}
