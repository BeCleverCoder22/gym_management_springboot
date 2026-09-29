package com.gym.management.gym_management.controller;

import com.gym.management.gym_management.dto.CustomerRequest;
import com.gym.management.gym_management.dto.CustomerResponse;
import com.gym.management.gym_management.dto.PageResponse;
import com.gym.management.gym_management.configuration.PaginationSupport;
import com.gym.management.gym_management.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Set;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/customers")
@Tag(name = "Clients")
@SecurityRequirement(name = "bearerAuth")
public class CustomerController {
    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @Operation(summary = "Rechercher et paginer les clients actifs")
    public PageResponse<CustomerResponse> getAllCustomers(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "registrationDate,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "firstName", "lastName", "registrationDate"));
        return PageResponse.from(
                customerService.searchCustomers(q, lastName, phone, pageable), CustomerResponse::from);
    }

    @GetMapping("/{id}")
    public CustomerResponse getCustomerById(@PathVariable Long id) {
        return CustomerResponse.from(customerService.getCustomerById(id));
    }

    @PostMapping
    @Operation(summary = "Créer un client")
    public ResponseEntity<CustomerResponse> addCustomer(@Valid @RequestBody CustomerRequest request) {
        CustomerResponse customer = CustomerResponse.from(customerService.addCustomer(
                request.firstName(), request.lastName(), request.phoneNumber()));
        return ResponseEntity.created(URI.create("/api/customers/" + customer.id())).body(customer);
    }

    @GetMapping("/search")
    public PageResponse<CustomerResponse> searchCustomers(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "registrationDate,desc") String sort) {
        Pageable pageable = PaginationSupport.create(
                page, size, sort, Set.of("id", "firstName", "lastName", "registrationDate"));
        return PageResponse.from(
                customerService.searchCustomers(null, lastName, null, pageable), CustomerResponse::from);
    }

    @PutMapping("/{id}")
    public CustomerResponse updateCustomer(
            @PathVariable Long id, @Valid @RequestBody CustomerRequest request) {
        return CustomerResponse.from(customerService.updateCustomer(
                id, request.firstName(), request.lastName(), request.phoneNumber()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Désactiver un client sans supprimer son historique")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.noContent().build();
    }
}
