package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;

import java.util.List;

public interface ICustomerService {
    public List<Customer> getAllCustomers();
    public Customer getCustomerById(Long id);
    public Customer addCustomer(Customer customer);
    public List<Customer> searchCustomersByLastName(String lastName);
    public Customer updateCustomer(Long id, Customer customer);
    public void deleteCustomer(Long id);
}
