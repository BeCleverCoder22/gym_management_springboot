package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = "organization")
    Optional<User> findByUsernameAndOrganization_Id(String username, Long organizationId);

    @EntityGraph(attributePaths = "organization")
    User findByUsernameAndOrganization_SlugIgnoreCase(String username, String organizationSlug);
    boolean existsByUsernameAndOrganization_Id(String username, Long organizationId);
    boolean existsByEmailIgnoreCaseAndOrganization_Id(String email, Long organizationId);
    org.springframework.data.domain.Page<User> findByOrganization_Id(Long organizationId,
            org.springframework.data.domain.Pageable pageable);
    Optional<User> findByIdAndOrganization_Id(Long id, Long organizationId);
    boolean existsByEmailIgnoreCase(String email);
}
