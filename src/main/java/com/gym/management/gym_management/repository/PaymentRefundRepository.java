package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.PaymentRefund;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface PaymentRefundRepository extends JpaRepository<PaymentRefund, Long> {
    Optional<PaymentRefund> findByIdAndOrganization_Id(Long id, Long organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("""
            SELECT r FROM PaymentRefund r
            WHERE r.id = :id AND r.organization.id = :organizationId
            """)
    Optional<PaymentRefund> findByIdForUpdate(
            @org.springframework.data.repository.query.Param("id") Long id,
            @org.springframework.data.repository.query.Param("organizationId") Long organizationId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT COALESCE(SUM(r.amount), 0) FROM PaymentRefund r
            WHERE r.payment.id = :paymentId
              AND r.organization.id = :organizationId
              AND r.status = com.gym.management.gym_management.entity.RefundStatus.REQUESTED
            """)
    java.math.BigDecimal sumRequestedRefunds(
            @org.springframework.data.repository.query.Param("paymentId") Long paymentId,
            @org.springframework.data.repository.query.Param("organizationId") Long organizationId);
}
