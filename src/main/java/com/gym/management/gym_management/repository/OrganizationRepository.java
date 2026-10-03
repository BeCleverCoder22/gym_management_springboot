package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findBySlugIgnoreCaseAndActiveTrue(String slug);
    Optional<Organization> findByIdAndActiveTrue(Long id);
    boolean existsBySlugIgnoreCase(String slug);
}
