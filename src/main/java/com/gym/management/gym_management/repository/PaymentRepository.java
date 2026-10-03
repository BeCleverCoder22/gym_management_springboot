package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrganization_IdAndIdempotencyKey(Long organizationId, String idempotencyKey);
    Optional<Payment> findByIdAndOrganization_Id(Long id, Long organizationId);
    boolean existsByOrganization_IdAndProviderReferenceAndIdNot(
            Long organizationId, String providerReference, Long paymentId);
    Page<Payment> findByOrganization_Id(Long organizationId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id AND p.organization.id = :organizationId")
    Optional<Payment> findByIdForUpdate(
            @Param("id") Long id, @Param("organizationId") Long organizationId);
}
