package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.CustomerRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService implements ICustomerService {
    private final CustomerRepository customerRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository, AuditService auditService) {
        this.customerRepository = customerRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(String search, String lastName, String phone, Pageable pageable) {
        return customerRepository.search(normalize(search), normalize(lastName), normalize(phone), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndEnabledTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable."));
    }

    @Override
    @Transactional
    public Customer addCustomer(String firstName, String lastName, String phoneNumber) {
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_CREATED", "CUSTOMER", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Customer updateCustomer(Long id, String firstName, String lastName, String phoneNumber) {
        Customer customer = getCustomerById(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_UPDATED", "CUSTOMER", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public void deleteCustomer(Long id) {
        Customer customer = getCustomerById(id);
        customer.setEnabled(false);
        customer.setActiveSubscription(false);
        customerRepository.save(customer);
        auditService.record("CUSTOMER_DEACTIVATED", "CUSTOMER", id);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
