package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ICustomerService {
    public Page<Customer> searchCustomers(String search, String lastName, String phone, Pageable pageable);
    public Customer getCustomerById(Long id);
    public Customer addCustomer(String firstName, String lastName, String phoneNumber, String email);
    public Customer updateCustomer(Long id, String firstName, String lastName, String phoneNumber, String email);
    public void deleteCustomer(Long id);
}
