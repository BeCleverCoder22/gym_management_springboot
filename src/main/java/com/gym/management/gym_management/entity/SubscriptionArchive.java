package com.gym.management.gym_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionArchive {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "pack_id", nullable = false)
    private Pack pack;

    private LocalDate startDate;
    private LocalDate endDate;
    private String offerNameAtPurchase;
    private BigDecimal monthlyPriceAtPurchase;
    private int durationMonthsAtPurchase;
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
    private Boolean isDeleted;  // Indiquer si l'abonnement a été supprimé
    private LocalDate deletionDate;  // Date de suppression

    // Méthode pour archiver un abonnement supprimé
    public void archiveSubscription(Subscription subscription) {
        this.customer = subscription.getCustomer();
        this.pack = subscription.getPack();
        this.startDate = subscription.getStartDate();
        this.endDate = subscription.getEndDate();
        this.offerNameAtPurchase = subscription.getOfferNameAtPurchase();
        this.monthlyPriceAtPurchase = subscription.getMonthlyPriceAtPurchase();
        this.durationMonthsAtPurchase = subscription.getDurationMonthsAtPurchase();
        this.status = SubscriptionStatus.CANCELLED;
        this.isDeleted = true;
        this.deletionDate = LocalDate.now(); // Date de suppression
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Pack getPack() {
        return pack;
    }

    public void setPack(Pack pack) {
        this.pack = pack;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Boolean getDeleted() {
        return isDeleted;
    }

    public void setDeleted(Boolean deleted) {
        isDeleted = deleted;
    }

    public LocalDate getDeletionDate() {
        return deletionDate;
    }

    public void setDeletionDate(LocalDate deletionDate) {
        this.deletionDate = deletionDate;
    }

    public String getOfferNameAtPurchase() {
        return offerNameAtPurchase != null ? offerNameAtPurchase : pack.getOfferName();
    }

    public BigDecimal getMonthlyPriceAtPurchase() {
        return monthlyPriceAtPurchase != null ? monthlyPriceAtPurchase : pack.getMonthlyPrice();
    }

    public int getDurationMonthsAtPurchase() {
        return durationMonthsAtPurchase > 0 ? durationMonthsAtPurchase : pack.getDurationMonths();
    }

    public SubscriptionStatus getStatus() {
        return status;
    }
}
