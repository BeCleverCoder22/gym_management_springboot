package com.gym.management.gym_management.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Objects;

@Component
public class JwtUtils {
    private final Key signingKey;
    private final long expirationTime;

    public JwtUtils(
            @Value("${app.secret-key}") String secretKey,
            @Value("${app.expiration-time}") long expirationTime) {
        if (expirationTime <= 0) {
            throw new IllegalArgumentException("JWT expiration must be positive.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, 0);
    }

    public String generateToken(String username, String role, int tokenVersion) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("tokenVersion", tokenVersion)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationTime))
                .signWith(signingKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        boolean matchingVersion = !(userDetails instanceof GymUserDetails gymUserDetails)
                || Objects.equals(claims.get("tokenVersion", Integer.class), gymUserDetails.getTokenVersion());
        return matchingVersion
                && Objects.equals(claims.getSubject(), userDetails.getUsername())
                && claims.getExpiration() != null
                && claims.getExpiration().after(new Date());
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
