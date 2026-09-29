package com.gym.management.gym_management.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(indexes = {
        @Index(name = "idx_customer_registration_date", columnList = "registration_date"),
        @Index(name = "idx_customer_name", columnList = "last_name,first_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String lastName;
    private String firstName;

    @Column(nullable = false, updatable = false)
    private LocalDate registrationDate;

    private String phoneNumber;

    private boolean activeSubscription;
    private Boolean enabled = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (registrationDate == null) {
            registrationDate = LocalDate.now();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = createdAt;
        }
        if (enabled == null) {
            enabled = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFirstName() {
        return firstName;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public boolean isActiveSubscription() {
        return activeSubscription;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setActiveSubscription(boolean activeSubscription) {
        this.activeSubscription = activeSubscription;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
