package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.gym.management.gym_management.repository.OrganizationRepository;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class CustomerService implements ICustomerService {
    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;

    public CustomerService(CustomerRepository customerRepository,
                           OrganizationRepository organizationRepository,
                           AuditService auditService) {
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Customer> searchCustomers(String search, String lastName, String phone, Pageable pageable) {
        Long organizationId = TenantContext.requireOrganizationId();
        String normalizedSearch = normalize(search);
        String normalizedLastName = normalize(lastName);
        String normalizedPhone = normalize(phone);

        Specification<Customer> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("organization").get("id"), organizationId));
            predicates.add(criteriaBuilder.or(
                    criteriaBuilder.isNull(root.get("enabled")),
                    criteriaBuilder.isTrue(root.get("enabled"))));

            if (normalizedSearch != null) {
                String pattern = containsPattern(normalizedSearch);
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("phoneNumber")), pattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), pattern)));
            }
            if (normalizedLastName != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")),
                        containsPattern(normalizedLastName)));
            }
            if (normalizedPhone != null) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("phoneNumber")),
                        containsPattern(normalizedPhone)));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };

        return customerRepository.findAll(specification, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        return customerRepository.findByIdAndOrganization_IdAndEnabledTrue(
                        id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable."));
    }

    @Override
    @Transactional
    public Customer addCustomer(String firstName, String lastName, String phoneNumber, String email) {
        Customer customer = new Customer();
        customer.setOrganization(organizationRepository.getReferenceById(
                TenantContext.requireOrganizationId()));
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
        Customer saved = customerRepository.save(customer);
        auditService.record("CUSTOMER_CREATED", "CUSTOMER", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Customer updateCustomer(Long id, String firstName, String lastName, String phoneNumber, String email) {
        Customer customer = getCustomerById(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setPhoneNumber(phoneNumber);
        customer.setEmail(email);
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
        return value == null || value.isBlank() ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String containsPattern(String value) {
        return "%" + value + "%";
    }
}
