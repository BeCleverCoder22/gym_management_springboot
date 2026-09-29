package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Pack;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PackRepository extends JpaRepository<Pack, Long> {
    Page<Pack> findByActiveTrue(Pageable pageable);
}
