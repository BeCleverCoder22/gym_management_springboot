package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    boolean existsByEmail(String email); // Vérifie si un email existe déjà
    boolean existsByUsername(String username); // Vérifie si un username existe déjà
}
