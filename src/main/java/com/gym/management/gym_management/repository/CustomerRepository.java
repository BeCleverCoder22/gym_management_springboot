package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDate;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    long countByRegistrationDateBetween(LocalDate startDate, LocalDate endDate);
    Optional<Customer> findByIdAndEnabledTrue(Long id);

    @Query(value = """
            SELECT c FROM Customer c
            WHERE (c.enabled IS NULL OR c.enabled = true)
              AND (:search IS NULL
                   OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
              AND (:lastName IS NULL
                   OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
              AND (:phone IS NULL
                   OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :phone, '%')))
            """,
            countQuery = """
                    SELECT COUNT(c) FROM Customer c
                    WHERE (c.enabled IS NULL OR c.enabled = true)
                      AND (:search IS NULL
                           OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%'))
                           OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :search, '%')))
                      AND (:lastName IS NULL
                           OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :lastName, '%')))
                      AND (:phone IS NULL
                           OR LOWER(c.phoneNumber) LIKE LOWER(CONCAT('%', :phone, '%')))
                    """)
    Page<Customer> search(
            @Param("search") String search,
            @Param("lastName") String lastName,
            @Param("phone") String phone,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Customer c
            WHERE c.id = :id AND (c.enabled IS NULL OR c.enabled = true)
            """)
    Optional<Customer> findByIdForUpdate(@Param("id") Long id);
}
