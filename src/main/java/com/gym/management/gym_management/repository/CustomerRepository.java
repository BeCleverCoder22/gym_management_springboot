package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.time.LocalDate;

public interface CustomerRepository extends JpaRepository<Customer, Long>, JpaSpecificationExecutor<Customer> {
    long countByOrganization_Id(Long organizationId);
    long countByOrganization_IdAndRegistrationDateBetween(
            Long organizationId, LocalDate startDate, LocalDate endDate);
    Optional<Customer> findByIdAndOrganization_IdAndEnabledTrue(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c FROM Customer c
            WHERE c.id = :id AND c.organization.id = :organizationId
              AND (c.enabled IS NULL OR c.enabled = true)
            """)
    Optional<Customer> findByIdForUpdate(
            @Param("id") Long id, @Param("organizationId") Long organizationId);

    boolean existsByIdAndOrganization_IdAndEnabledTrue(Long id, Long organizationId);
}
