package com.gym.management.gym_management.configuration;

import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.repository.UserRepository;
import com.gym.management.gym_management.repository.OrganizationRepository;
import com.gym.management.gym_management.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
@Profile({"dev", "prod"})
public class AdminBootstrapRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final String username;
    private final String email;
    private final String password;
    private final String organizationName;
    private final String organizationSlug;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService,
            @Value("${APP_BOOTSTRAP_ADMIN_USERNAME:}") String username,
            @Value("${APP_BOOTSTRAP_ADMIN_EMAIL:}") String email,
            @Value("${APP_BOOTSTRAP_ADMIN_PASSWORD:}") String password,
            @Value("${APP_BOOTSTRAP_ORGANIZATION_NAME:Gym Management}") String organizationName,
            @Value("${APP_BOOTSTRAP_ORGANIZATION_SLUG:gym-management}") String organizationSlug) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.username = username;
        this.email = email;
        this.password = password;
        this.organizationName = organizationName;
        this.organizationSlug = organizationSlug;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        boolean configured = !username.isBlank() || !email.isBlank() || !password.isBlank();
        if (!configured) {
            return;
        }
        if (username.isBlank() || email.isBlank() || password.length() < 12 || password.length() > 72
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "Initial admin requires username, email, and a password of 12 to 72 characters.");
        }
        if (userRepository.count() != 0) {
            log.info("Initial admin bootstrap skipped because users already exist.");
            return;
        }

        Organization organization = organizationRepository.findBySlugIgnoreCaseAndActiveTrue(organizationSlug)
                .orElseGet(() -> organizationRepository.save(
                        new Organization(organizationName, organizationSlug)));
        User admin = new User();
        admin.setOrganization(organization);
        admin.setUsername(username.trim());
        admin.setEmail(email.trim().toLowerCase(Locale.ROOT));
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setEnabled(true);
        User saved = userRepository.save(admin);
        auditService.recordSystem(organization.getId(), "ADMIN_BOOTSTRAPPED", "USER", saved.getId());
        log.info("Initial administrator account created.");
    }
}
