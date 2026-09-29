package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.configuration.JwtUtils;
import com.gym.management.gym_management.dto.AuthRequest;
import com.gym.management.gym_management.dto.LoginRequest;
import com.gym.management.gym_management.dto.TokenResponse;
import com.gym.management.gym_management.dto.UserResponse;
import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.exception.InvalidCredentialsException;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication")
public class AuthController {

    private final CustomUserDetailsService userService;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;

    public AuthController(CustomUserDetailsService userService, JwtUtils jwtUtils,
                          AuthenticationManager authenticationManager) {
        this.userService = userService;
        this.jwtUtils = jwtUtils;
        this.authenticationManager = authenticationManager;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte utilisateur public (rôle USER)")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody AuthRequest request) {
        User registeredUser = userService.register(request.username(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(registeredUser));
    }

    @PostMapping("/login")
    @Operation(summary = "S'authentifier et obtenir un access token JWT")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (AuthenticationException exception) {
            userService.recordFailedLogin(request.username());
            throw new InvalidCredentialsException();
        }

        User user = userService.recordSuccessfulLogin(request.username());
        String role = user.getRole().name();
        String token = jwtUtils.generateToken(user.getUsername(), role, user.getTokenVersion());
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", role));
    }

    @PostMapping("/logout")
    @Operation(summary = "Révoquer les access tokens actifs du compte",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<Void> logout() {
        userService.revokeCurrentUserTokens();
        return ResponseEntity.noContent().build();
    }
}
