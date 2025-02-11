package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.repository.CustomerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerService implements ICustomerService{
    @Autowired
    private CustomerRepository customerRepository;

    @Override
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @Override
    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found with id: " + id));
    }


    @Override
    public Customer addCustomer(Customer customer) {
        return customerRepository.save(customer);
    }

    @Override
    public List<Customer> searchCustomersByLastName(String lastName) {
        return customerRepository.findByLastNameContainingIgnoreCase(lastName);
    }

    @Override
    public Customer updateCustomer(Long id, Customer customer) {
        return customerRepository.findById(id).map(existingCustomer -> {
            if (customer.getFirstName() != null) {
                existingCustomer.setFirstName(customer.getFirstName());
            }
            if (customer.getLastName() != null) {
                existingCustomer.setLastName(customer.getLastName());
            }
            if (customer.getPhoneNumber() != null) {
                existingCustomer.setPhoneNumber(customer.getPhoneNumber());
            }
            if (customer.isActiveSubscription() != existingCustomer.isActiveSubscription()) {
                existingCustomer.setActiveSubscription(customer.isActiveSubscription());
            }
            return customerRepository.save(existingCustomer);
        }).orElseThrow(() -> new RuntimeException("Customer not found"));
    }


    @Override
    public void deleteCustomer(Long id) {
        customerRepository.deleteById(id);
    }

}
