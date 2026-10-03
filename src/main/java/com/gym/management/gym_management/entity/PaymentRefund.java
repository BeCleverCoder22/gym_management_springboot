package com.gym.management.gym_management.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(indexes = @Index(name = "idx_refund_payment", columnList = "payment_id,status"))
public class PaymentRefund {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 500)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant completedAt;

    protected PaymentRefund() {
    }

    public PaymentRefund(Organization organization, Payment payment, BigDecimal amount, String reason) {
        this.organization = organization;
        this.payment = payment;
        this.amount = amount;
        this.reason = reason;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void complete() {
        if (status != RefundStatus.REQUESTED) {
            throw new IllegalStateException("Only requested refunds can be completed.");
        }
        status = RefundStatus.COMPLETED;
        completedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Payment getPayment() { return payment; }
    public BigDecimal getAmount() { return amount; }
    public String getReason() { return reason; }
    public RefundStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }
}
