package com.gym.management.gym_management.configuration;

import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.security.GymUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import io.jsonwebtoken.security.WeakKeyException;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {
    private static final String SIGNING_KEY = generateSigningKey();

    @Test
    void generatedTokenIsSignedAndBoundToItsSubject() {
        JwtUtils jwtUtils = new JwtUtils(SIGNING_KEY, 60_000);
        String token = jwtUtils.generateToken("trainer", "USER");
        UserDetails matchingUser = user("trainer");
        UserDetails differentUser = user("another-trainer");

        assertEquals("trainer", jwtUtils.extractUsername(token));
        assertTrue(jwtUtils.validateToken(token, matchingUser));
        assertFalse(jwtUtils.validateToken(token, differentUser));
    }

    @Test
    void rejectsWeakSigningKeysAndNonPositiveExpiration() {
        assertThrows(WeakKeyException.class, () -> new JwtUtils("short", 60_000));
        assertThrows(IllegalArgumentException.class, () -> new JwtUtils(SIGNING_KEY, 0));
    }

    @Test
    void tokenVersionRevokesTokensAfterLogoutOrCredentialChanges() {
        JwtUtils jwtUtils = new JwtUtils(SIGNING_KEY, 60_000);
        User user = new User();
        user.setUsername("trainer");
        user.setPassword("encoded");
        user.setRole(UserRole.USER);
        user.setTokenVersion(4);
        GymUserDetails principal = new GymUserDetails(user, java.time.Clock.systemUTC());

        String currentToken = jwtUtils.generateToken("trainer", "USER", 4);
        String revokedToken = jwtUtils.generateToken("trainer", "USER", 3);

        assertTrue(jwtUtils.validateToken(currentToken, principal));
        assertFalse(jwtUtils.validateToken(revokedToken, principal));
    }

    private UserDetails user(String username) {
        return org.springframework.security.core.userdetails.User.withUsername(username)
                .password("unused").authorities("USER").build();
    }

    private static String generateSigningKey() {
        byte[] key = new byte[48];
        new java.security.SecureRandom().nextBytes(key);
        return java.util.HexFormat.of().formatHex(key);
    }
}
