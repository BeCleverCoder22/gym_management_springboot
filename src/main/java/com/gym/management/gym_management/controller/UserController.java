package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.AdminUserRequest;
import com.gym.management.gym_management.dto.AdminUserUpdateRequest;
import com.gym.management.gym_management.dto.ChangePasswordRequest;
import com.gym.management.gym_management.dto.ProfileUpdateRequest;
import com.gym.management.gym_management.dto.UserResponse;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {
    private final CustomUserDetailsService userService;

    public UserController(CustomUserDetailsService userService) {
        this.userService = userService;
    }

    @GetMapping
    public PageResponse<UserResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "username", "email", "createdAt", "lastLogin"));
        return PageResponse.from(userService.getAllUsers(pageable), UserResponse::from);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody AdminUserRequest request) {
        User user = userService.createUser(
                request.username(), request.email(), request.password(), request.role()
        );
        return ResponseEntity.status(201).body(UserResponse.from(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody AdminUserUpdateRequest request) {
        User user = userService.updateUser(
                id, request.username(), request.email(), request.role()
        );
        return ResponseEntity.ok(UserResponse.from(user));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un utilisateur (administrateur)")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        return ResponseEntity.ok(UserResponse.from(userService.getCurrentUser()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(UserResponse.from(userService.updateProfile(request.email())));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.oldPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }
}
