package com.gym.management.gym_management.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_idempotency", columnNames = {"organization_id", "idempotency_key"}),
        indexes = {
                @Index(name = "idx_payment_org_created", columnList = "organization_id,created_at"),
                @Index(name = "idx_payment_subscription", columnList = "subscription_id")
        })
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "idempotency_key", nullable = false, length = 100)
    private String idempotencyKey;

    @Column(length = 160)
    private String providerReference;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant settledAt;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    protected Payment() {
    }

    public Payment(
            Organization organization, Subscription subscription, BigDecimal amount,
            String currency, PaymentMethod method, String idempotencyKey) {
        this.organization = organization;
        this.subscription = subscription;
        this.amount = amount;
        this.currency = currency;
        this.method = method;
        this.idempotencyKey = idempotencyKey;
        this.status = PaymentStatus.PENDING;
        this.refundedAmount = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }

    public void complete(String providerReference) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be completed.");
        }
        status = PaymentStatus.COMPLETED;
        settledAt = Instant.now();
        this.providerReference = providerReference;
    }

    public void recordRefund(BigDecimal refundAmount) {
        if (status != PaymentStatus.COMPLETED && status != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new IllegalStateException("Only settled payments can be refunded.");
        }
        if (refundAmount.signum() <= 0 || refundedAmount.add(refundAmount).compareTo(amount) > 0) {
            throw new IllegalArgumentException("Refund amount exceeds the refundable balance.");
        }
        refundedAmount = refundedAmount.add(refundAmount);
        status = refundedAmount.compareTo(amount) == 0
                ? PaymentStatus.REFUNDED
                : PaymentStatus.PARTIALLY_REFUNDED;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public Subscription getSubscription() { return subscription; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public PaymentMethod getMethod() { return method; }
    public PaymentStatus getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getProviderReference() { return providerReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getSettledAt() { return settledAt; }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
}
