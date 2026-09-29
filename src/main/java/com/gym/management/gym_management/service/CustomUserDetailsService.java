package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.UserRepository;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.time.Clock;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final Clock clock;

    public CustomUserDetailsService(
            UserRepository userRepository,
            @Lazy PasswordEncoder passwordEncoder,
            AuditService auditService,
            Clock clock) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username);

        if (user == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new GymUserDetails(user, clock);
    }


    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé."));
    }

    public User findByUsername(String username) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé");
        }
        return user;
    }

    public boolean usernameOrEmailExists(String username, String email) {
        return userRepository.existsByUsername(username.trim())
                || userRepository.existsByEmailIgnoreCase(normalizeEmail(email));
    }

    @Transactional
    public User recordSuccessfulLogin(String username) {
        User user = findByUsername(username);
        user.setLastLogin(LocalDateTime.now(clock));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastFailedLoginAt(null);
        User saved = userRepository.save(user);
        auditService.record("USER_LOGIN", "USER", saved.getId());
        return saved;
    }

    @Transactional
    public void recordFailedLogin(String username) {
        User user = userRepository.findByUsernameForUpdate(username).orElse(null);
        if (user == null || (user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now(clock)))) {
            return;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (user.getLastFailedLoginAt() == null
                || user.getLastFailedLoginAt().isBefore(now.minus(Duration.ofMinutes(15)))) {
            user.setFailedLoginAttempts(0);
        }
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        user.setLastFailedLoginAt(now);
        if (attempts >= 5) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plus(Duration.ofMinutes(15)));
        }
        userRepository.save(user);
    }

    @Transactional
    public User register(String username, String email, String rawPassword) {
        validatePassword(rawPassword);
        username = username.trim();
        email = normalizeEmail(email);
        if (usernameOrEmailExists(username, email)) {
            throw new ConflictException("Le nom d'utilisateur ou l'email est déjà utilisé.");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        User saved = userRepository.save(user);
        auditService.record("USER_REGISTERED", "USER", saved.getId());
        return saved;
    }

    @Transactional
    public User createUser(String username, String email, String rawPassword, String role) {
        validatePassword(rawPassword);
        username = username.trim();
        email = normalizeEmail(email);
        if (usernameOrEmailExists(username, email)) {
            throw new ConflictException("Le nom d'utilisateur ou l'email est déjà utilisé.");
        }
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(UserRole.valueOf(role));
        user.setEnabled(true);
        User saved = userRepository.save(user);
        auditService.record("USER_CREATED", "USER", saved.getId());
        auditService.record("USER_ROLE_ASSIGNED", "USER", saved.getId());
        return saved;
    }

    @Transactional
    public User updateUser(Long id, String username, String email, String role) {
        User existingUser = getUserById(id);
        UserRole previousRole = existingUser.getRole();
        username = username == null ? null : username.trim();
        email = email == null ? null : normalizeEmail(email);
        if (username != null && !username.isBlank() && !username.equals(existingUser.getUsername())
                && userRepository.existsByUsername(username)) {
            throw new ConflictException("Le nom d'utilisateur est déjà pris.");
        }
        if (email != null && !email.isBlank() && !email.equals(existingUser.getEmail())
                && userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("L'email est déjà utilisé.");
        }
        if (username != null && !username.isBlank()) existingUser.setUsername(username);
        if (email != null && !email.isBlank()) existingUser.setEmail(email);
        if (role != null && !role.isBlank()) existingUser.setRole(UserRole.valueOf(role));
        if (role != null && !role.isBlank() && !UserRole.valueOf(role).equals(previousRole)) {
            existingUser.setTokenVersion(existingUser.getTokenVersion() + 1);
        }
        User saved = userRepository.save(existingUser);
        auditService.record("USER_UPDATED", "USER", id);
        if (role != null && !role.isBlank() && !UserRole.valueOf(role).equals(previousRole)) {
            auditService.record("USER_ROLE_CHANGED", "USER", id);
        }
        return saved;
    }

    @Transactional
    public void deactivateUser(Long id) {
        User user = getUserById(id);
        user.setEnabled(false);
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record("USER_DEACTIVATED", "USER", id);
    }

    @Transactional
    public void revokeCurrentUserTokens() {
        User user = findByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record("USER_LOGOUT", "USER", user.getId());
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username); // Récupère l'utilisateur actuellement connecté.
    }


    @Transactional
    public User updateProfile(String email) {
        email = normalizeEmail(email);
        User user = getCurrentUser();
        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé !");
        }
        if (userRepository.existsByEmailIgnoreCase(email) && !email.equalsIgnoreCase(user.getEmail())) {
            throw new ConflictException("L'email est déjà utilisé.");
        }
        user.setEmail(email);
        User updated = userRepository.save(user);
        auditService.record("USER_PROFILE_UPDATED", "USER", user.getId());
        return updated;
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) { // Change le mot de passe de l'utilisateur après vérification.
        validatePassword(newPassword);
        User user = getCurrentUser();
        if (user == null) {
            throw new UsernameNotFoundException("Utilisateur non trouvé !");
        }

        // Vérifier si l'ancien mot de passe est correct
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect !");
        }

        // Mettre à jour le mot de passe
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record("USER_PASSWORD_CHANGED", "USER", user.getId());
    }

    private void validatePassword(String password) {
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas dépasser 72 octets UTF-8.");
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
