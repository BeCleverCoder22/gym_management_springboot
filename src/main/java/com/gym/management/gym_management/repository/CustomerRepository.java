package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByLastNameContainingIgnoreCase(String lastName);
    long countByActiveSubscription(boolean active);
}
