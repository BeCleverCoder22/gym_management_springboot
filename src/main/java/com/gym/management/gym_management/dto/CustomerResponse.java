package com.gym.management.gym_management.dto;

import com.gym.management.gym_management.entity.Customer;

import java.time.LocalDate;

public record CustomerResponse(
        Long id,
        String firstName,
        String lastName,
        LocalDate registrationDate,
        String phoneNumber,
        String email,
        boolean activeSubscription,
        boolean enabled
) {
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getRegistrationDate(),
                customer.getPhoneNumber(),
                customer.getEmail(),
                customer.isActiveSubscription(),
                !Boolean.FALSE.equals(customer.getEnabled())
        );
    }
}
