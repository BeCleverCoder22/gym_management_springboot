package com.gym.management.gym_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(indexes = {
        @Index(name = "idx_subscription_customer_start", columnList = "customer_id,start_date"),
        @Index(name = "idx_subscription_status_end", columnList = "status,end_date"),
        @Index(name = "idx_subscription_start_date", columnList = "start_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "pack_id", nullable = false)
    private Pack pack;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private int durationMonthsAtPurchase;

    @Column(precision = 12, scale = 2)
    private BigDecimal monthlyPriceAtPurchase;

    @Column(length = 100)
    private String offerNameAtPurchase;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private SubscriptionStatus status = SubscriptionStatus.SCHEDULED;

    public void setPack(Pack pack) {
        this.pack = pack;
        if (pack != null) {
            this.durationMonthsAtPurchase = pack.getDurationMonths();
            this.monthlyPriceAtPurchase = pack.getMonthlyPrice();
            this.offerNameAtPurchase = pack.getOfferName();
            updateEndDate();
        }
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        updateEndDate();
    }

    @PrePersist
    @PreUpdate
    private void preservePackTerms() {
        if (pack != null) {
            if (durationMonthsAtPurchase <= 0) {
                durationMonthsAtPurchase = pack.getDurationMonths();
            }
            if (monthlyPriceAtPurchase == null) {
                monthlyPriceAtPurchase = pack.getMonthlyPrice();
            }
            if (offerNameAtPurchase == null) {
                offerNameAtPurchase = pack.getOfferName();
            }
        }
        updateEndDate();
    }

    private void updateEndDate() {
        if (startDate != null && durationMonthsAtPurchase > 0) {
            endDate = startDate.plusMonths(durationMonthsAtPurchase);
        }
    }

    public LocalDate getEndDate() {
        if (endDate != null) {
            return endDate;
        }
        if (startDate != null && durationMonthsAtPurchase > 0) {
            return startDate.plusMonths(durationMonthsAtPurchase);
        }
        if (startDate != null && pack != null) {
            return startDate.plusMonths(pack.getDurationMonths());
        }
        return null;
    }

    public int getDurationMonthsAtPurchase() {
        return durationMonthsAtPurchase > 0
                ? durationMonthsAtPurchase
                : pack == null ? 0 : pack.getDurationMonths();
    }

    public BigDecimal getMonthlyPriceAtPurchase() {
        return monthlyPriceAtPurchase != null
                ? monthlyPriceAtPurchase
                : pack == null ? null : pack.getMonthlyPrice();
    }

    public String getOfferNameAtPurchase() {
        return offerNameAtPurchase != null
                ? offerNameAtPurchase
                : pack == null ? null : pack.getOfferName();
    }

    public SubscriptionStatus getStatus() {
        if (status != null) {
            return status;
        }
        LocalDate calculatedEndDate = getEndDate();
        if (startDate == null || calculatedEndDate == null) {
            return SubscriptionStatus.SCHEDULED;
        }
        LocalDate today = LocalDate.now();
        if (calculatedEndDate.isBefore(today)) {
            return SubscriptionStatus.EXPIRED;
        }
        return startDate.isAfter(today) ? SubscriptionStatus.SCHEDULED : SubscriptionStatus.ACTIVE;
    }

    public SubscriptionStatus getStoredStatus() {
        return status;
    }
}
