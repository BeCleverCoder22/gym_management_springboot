package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.OrganizationRepository;
import com.gym.management.gym_management.repository.UserRepository;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final NotificationOutboxService notificationService;
    private final Clock clock;

    public CustomUserDetailsService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            @Lazy PasswordEncoder passwordEncoder,
            AuditService auditService,
            NotificationOutboxService notificationService,
            Clock clock) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    @Override
    public UserDetails loadUserByUsername(String loginIdentifier) throws UsernameNotFoundException {
        int separator = loginIdentifier.indexOf("::");
        if (separator <= 0 || separator == loginIdentifier.length() - 2) {
            throw new UsernameNotFoundException("Invalid login identifier.");
        }
        User user = userRepository.findByUsernameAndOrganization_SlugIgnoreCase(
                loginIdentifier.substring(separator + 2), loginIdentifier.substring(0, separator));
        if (user == null) {
            throw new UsernameNotFoundException("User not found.");
        }
        return new GymUserDetails(user, clock);
    }

    public UserDetails loadUserByOrganization(String username, Long organizationId) {
        User user = userRepository.findByUsernameAndOrganization_Id(username, organizationId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found."));
        return new GymUserDetails(user, clock);
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findByOrganization_Id(TenantContext.requireOrganizationId(), pageable);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findByIdAndOrganization_Id(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    @Transactional(readOnly = true)
    public User findByUsernameAndOrganization(String username, String slug) {
        User user = userRepository.findByUsernameAndOrganization_SlugIgnoreCase(username, slug);
        if (user == null) {
            throw new UsernameNotFoundException("User not found.");
        }
        return user;
    }

    @Transactional
    public User register(
            String organizationName, String organizationSlug,
            String username, String email, String rawPassword) {
        validatePassword(rawPassword);
        String slug = organizationSlug.trim().toLowerCase(Locale.ROOT);
        if (organizationRepository.existsBySlugIgnoreCase(slug)) {
            throw new ConflictException("Cet identifiant d'organisation est déjà utilisé.");
        }
        Organization organization = organizationRepository.save(
                new Organization(organizationName.trim(), slug));
        String normalizedUsername = username.trim();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByUsernameAndOrganization_Id(normalizedUsername, organization.getId())
                || userRepository.existsByEmailIgnoreCaseAndOrganization_Id(
                normalizedEmail, organization.getId())) {
            throw new ConflictException("Le nom d'utilisateur ou l'email est déjà utilisé.");
        }
        User user = newUser(organization, normalizedUsername, normalizedEmail, rawPassword, UserRole.ADMIN);
        User saved = userRepository.save(user);
        auditService.recordOrganization(
                organization.getId(), "ORGANIZATION_CREATED", "ORGANIZATION", organization.getId());
        auditService.recordOrganization(organization.getId(), "USER_REGISTERED", "USER", saved.getId());
        notificationService.welcomeOrganization(organization, saved.getEmail());
        return saved;
    }

    @Transactional
    public User createUser(String username, String email, String rawPassword, String role) {
        validatePassword(rawPassword);
        Long organizationId = TenantContext.requireOrganizationId();
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable."));
        String normalizedUsername = username.trim();
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsByUsernameAndOrganization_Id(normalizedUsername, organizationId)
                || userRepository.existsByEmailIgnoreCaseAndOrganization_Id(normalizedEmail, organizationId)) {
            throw new ConflictException("Le nom d'utilisateur ou l'email est déjà utilisé.");
        }
        User saved = userRepository.save(newUser(
                organization, normalizedUsername, normalizedEmail, rawPassword, UserRole.valueOf(role)));
        auditService.record("USER_CREATED", "USER", saved.getId());
        auditService.record("USER_ROLE_ASSIGNED", "USER", saved.getId());
        return saved;
    }

    @Transactional
    public User updateUser(Long id, String username, String email, String role) {
        User user = getUserById(id);
        Long organizationId = TenantContext.requireOrganizationId();
        UserRole previousRole = user.getRole();
        String normalizedUsername = username == null ? null : username.trim();
        String normalizedEmail = email == null ? null : normalizeEmail(email);
        if (normalizedUsername != null && !normalizedUsername.isBlank()
                && !normalizedUsername.equals(user.getUsername())
                && userRepository.existsByUsernameAndOrganization_Id(normalizedUsername, organizationId)) {
            throw new ConflictException("Le nom d'utilisateur est déjà pris.");
        }
        if (normalizedEmail != null && !normalizedEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndOrganization_Id(normalizedEmail, organizationId)) {
            throw new ConflictException("L'email est déjà utilisé.");
        }
        if (normalizedUsername != null && !normalizedUsername.isBlank()) {
            user.setUsername(normalizedUsername);
        }
        if (normalizedEmail != null) {
            user.setEmail(normalizedEmail);
        }
        if (role != null && !role.isBlank()) {
            user.setRole(UserRole.valueOf(role));
        }
        if (user.getRole() != previousRole) {
            user.setTokenVersion(user.getTokenVersion() + 1);
            auditService.record("USER_ROLE_CHANGED", "USER", id);
        }
        User saved = userRepository.save(user);
        auditService.record("USER_UPDATED", "USER", id);
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
    public User recordSuccessfulLogin(String username, String organizationSlug) {
        User user = findByUsernameAndOrganization(username, organizationSlug);
        user.setLastLogin(LocalDateTime.now(clock));
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastFailedLoginAt(null);
        User saved = userRepository.save(user);
        auditService.recordOrganization(
                user.getOrganization().getId(), "USER_LOGIN", "USER", user.getId());
        return saved;
    }

    @Transactional
    public void recordFailedLogin(String username, String organizationSlug) {
        User user = userRepository.findByUsernameAndOrganization_SlugIgnoreCase(username, organizationSlug);
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
        user.setLastFailedLoginAt(now);
        if (attempts >= 5) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(now.plus(Duration.ofMinutes(15)));
        } else {
            user.setFailedLoginAttempts(attempts);
        }
        userRepository.save(user);
    }

    @Transactional
    public void revokeCurrentUserTokens() {
        User user = getCurrentUser();
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record("USER_LOGOUT", "USER", user.getId());
    }

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        GymUserDetails principal = (GymUserDetails) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        return userRepository.findByUsernameAndOrganization_Id(
                        principal.getUsername(), principal.getOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable."));
    }

    @Transactional
    public User updateProfile(String email) {
        User user = getCurrentUser();
        String normalizedEmail = normalizeEmail(email);
        Long organizationId = TenantContext.requireOrganizationId();
        if (!normalizedEmail.equalsIgnoreCase(user.getEmail())
                && userRepository.existsByEmailIgnoreCaseAndOrganization_Id(normalizedEmail, organizationId)) {
            throw new ConflictException("L'email est déjà utilisé.");
        }
        user.setEmail(normalizedEmail);
        User updated = userRepository.save(user);
        auditService.record("USER_PROFILE_UPDATED", "USER", user.getId());
        return updated;
    }

    @Transactional
    public void changePassword(String oldPassword, String newPassword) {
        validatePassword(newPassword);
        User user = getCurrentUser();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("Ancien mot de passe incorrect !");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setTokenVersion(user.getTokenVersion() + 1);
        userRepository.save(user);
        auditService.record("USER_PASSWORD_CHANGED", "USER", user.getId());
    }

    private User newUser(
            Organization organization, String username, String email,
            String rawPassword, UserRole role) {
        User user = new User();
        user.setOrganization(organization);
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        return user;
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
