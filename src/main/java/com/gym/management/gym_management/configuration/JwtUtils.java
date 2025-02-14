package com.gym.management.gym_management.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    @Value("${app.secret-key}")
    private String secretKey;

    @Value("${app.expiration-time}")
    private Long expirationTime;

    /**
     * 🔹 Générer un token avec username et rôle
     */
    public String generateToken(String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", role); // ✅ Ajoute le rôle au payload du token
        return createToken(claims, username);
    }

    /**
     * 🔹 Créer un token JWT
     */
    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime)) // ✅ Expiration correcte
                .signWith(getSignKey(), SignatureAlgorithm.HS256) // ✅ Signature sécurisée
                .compact();
    }

    /**
     * 🔹 Générer une clé de signature sécurisée
     */
    private Key getSignKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes()); // ✅ Génère une clé HMAC SHA-256 correcte
    }

    /**
     * 🔹 Vérifier si le token est valide
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    /**
     * 🔹 Vérifier si le token est expiré
     */
    private boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

    /**
     * 🔹 Extraire le username depuis le token
     */
    public String extractUsername(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    /**
     * 🔹 Extraire le rôle depuis le token
     */
    public String extractRole(String token) {
        return extractClaims(token, claims -> (String) claims.get("role")); // ✅ Extraction du rôle
    }

    /**
     * 🔹 Extraire la date d'expiration
     */
    private Date extractExpirationDate(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    /**
     * 🔹 Extraire les claims génériques
     */
    private <T> T extractClaims(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 🔹 Extraire tous les claims du token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignKey()) // ✅ Utilise la clé correcte pour parser le token
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
