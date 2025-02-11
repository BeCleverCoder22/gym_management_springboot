package com.gym.management.gym_management.entity;

import jakarta.persistence.*;
import lombok.*;
import com.gym.management.gym_management.entity.Customer;

import java.time.LocalDate;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "pack_id", nullable = false)
    private Pack pack;

    private LocalDate startDate;

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Long getId() {
        return id;
    }

    public Pack getPack() {
        return pack;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return startDate.plusMonths(pack.getDurationMonths()); // Calcul automatique
    }

    public void setPack(Pack pack) {
        this.pack = pack;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }
}
