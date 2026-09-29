package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.PackRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private PackRepository packRepository;
    @Mock private SubscriptionArchiveRepository archiveRepository;
    @Mock private AuditService auditService;

    private SubscriptionService service;
    private Customer customer;
    private Pack pack;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository, customerRepository, packRepository,
                archiveRepository, auditService, Clock.systemUTC());
        customer = new Customer();
        customer.setId(10L);
        customer.setEnabled(true);
        pack = new Pack();
        pack.setId(20L);
        pack.setOfferName("Monthly");
        pack.setDurationMonths(1);
        pack.setMonthlyPrice(new BigDecimal("49.90"));
        pack.setActive(true);
        when(customerRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(customer));
        when(packRepository.findById(20L)).thenReturn(Optional.of(pack));
    }

    @Test
    void createsActiveSubscriptionWithSnapshotAndCalculatedDates() {
        LocalDate startDate = LocalDate.now(Clock.systemUTC());
        when(subscriptionRepository.existsOverlappingSubscription(
                anyLong(), any(), any(), anySet())).thenReturn(false);
        when(subscriptionRepository.hasActiveSubscription(anyLong(), any(), any())).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription subscription = invocation.getArgument(0);
            subscription.setId(30L);
            return subscription;
        });

        Subscription created = service.addSubscription(10L, 20L, startDate);

        assertEquals(SubscriptionStatus.ACTIVE, created.getStatus());
        assertEquals(startDate.plusMonths(1), created.getEndDate());
        assertEquals(new BigDecimal("49.90"), created.getMonthlyPriceAtPurchase());
        assertEquals("Monthly", created.getOfferNameAtPurchase());
        verify(auditService).record("SUBSCRIPTION_CREATED", "SUBSCRIPTION", 30L);
    }

    @Test
    void rejectsAnOverlappingSubscriptionBeforeSaving() {
        when(subscriptionRepository.existsOverlappingSubscription(
                eq(10L), any(), any(), eq(Set.of(SubscriptionStatus.SCHEDULED, SubscriptionStatus.ACTIVE))))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.addSubscription(10L, 20L, LocalDate.now(Clock.systemUTC())));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }
}
