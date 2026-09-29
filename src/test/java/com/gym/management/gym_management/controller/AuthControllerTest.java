package com.gym.management.gym_management.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gym.management.gym_management.configuration.JwtUtils;
import com.gym.management.gym_management.dto.AuthRequest;
import com.gym.management.gym_management.dto.UserResponse;
import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.service.CustomUserDetailsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {
    private static final String TEST_PASSWORD = java.util.UUID.randomUUID() + "-A1";
    @Mock
    private CustomUserDetailsService userService;
    @Mock
    private JwtUtils jwtUtils;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthController controller;

    @Test
    void publicRegistrationReturnsSafeUserResponseAndNoRoleInput() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setUsername("new-user");
        user.setEmail("user@example.com");
        user.setPassword("$2a$12$hashed-password");
        user.setRole(UserRole.USER);
        when(userService.register("new-user", "user@example.com", TEST_PASSWORD))
                .thenReturn(user);

        var response = controller.register(
                new AuthRequest("new-user", "user@example.com", TEST_PASSWORD));

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("USER", response.getBody().role());
        String json = new ObjectMapper().writeValueAsString(response.getBody());
        assertFalse(json.contains("password"));
        assertFalse(json.contains("$2a$12$"));
        assertTrue(java.util.Arrays.stream(AuthRequest.class.getRecordComponents())
                .noneMatch(component -> component.getName().equals("role")));
        verify(userService).register("new-user", "user@example.com", TEST_PASSWORD);
    }
}
