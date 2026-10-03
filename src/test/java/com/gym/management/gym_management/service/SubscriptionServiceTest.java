package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.entity.User;
import com.gym.management.gym_management.entity.UserRole;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.PackRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import com.gym.management.gym_management.repository.OrganizationRepository;
import com.gym.management.gym_management.security.GymUserDetails;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
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
    @Mock private OrganizationRepository organizationRepository;
    @Mock private NotificationOutboxService notificationService;

    private SubscriptionService service;
    private Customer customer;
    private Pack pack;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository, customerRepository, packRepository,
                archiveRepository, auditService, organizationRepository, Clock.systemUTC(),
                notificationService);
        organization = new Organization("Gym", "gym");
        org.springframework.test.util.ReflectionTestUtils.setField(organization, "id", 7L);
        User user = new User();
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRole(UserRole.ADMIN);
        user.setOrganization(organization);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        new GymUserDetails(user, Clock.systemUTC()), "n/a"));
        customer = new Customer();
        customer.setId(10L);
        customer.setEnabled(true);
        customer.setOrganization(organization);
        pack = new Pack();
        pack.setId(20L);
        pack.setOrganization(organization);
        pack.setOfferName("Monthly");
        pack.setDurationMonths(1);
        pack.setMonthlyPrice(new BigDecimal("49.90"));
        pack.setActive(true);
    }

    @Test
    void createsActiveSubscriptionWithSnapshotAndCalculatedDates() {
        when(organizationRepository.getReferenceById(7L)).thenReturn(organization);
        when(customerRepository.findByIdForUpdate(10L, 7L)).thenReturn(Optional.of(customer));
        when(packRepository.findByIdAndOrganization_Id(20L, 7L)).thenReturn(Optional.of(pack));
        LocalDate startDate = LocalDate.now(Clock.systemUTC());
        when(subscriptionRepository.existsOverlappingSubscription(
                anyLong(), anyLong(), any(LocalDate.class), any(LocalDate.class), anySet())).thenReturn(false);
        when(subscriptionRepository.hasActiveSubscription(
                anyLong(), anyLong(), any(), any(LocalDate.class))).thenReturn(false);
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
        verify(notificationService).subscriptionCreated(created);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsAnOverlappingSubscriptionBeforeSaving() {
        when(organizationRepository.getReferenceById(7L)).thenReturn(organization);
        when(customerRepository.findByIdForUpdate(10L, 7L)).thenReturn(Optional.of(customer));
        when(packRepository.findByIdAndOrganization_Id(20L, 7L)).thenReturn(Optional.of(pack));
        when(subscriptionRepository.existsOverlappingSubscription(
                eq(7L), eq(10L), any(LocalDate.class), any(LocalDate.class),
                eq(Set.of(SubscriptionStatus.SCHEDULED, SubscriptionStatus.ACTIVE))))
                .thenReturn(true);

        assertThrows(ConflictException.class,
                () -> service.addSubscription(10L, 20L, LocalDate.now(Clock.systemUTC())));

        verify(subscriptionRepository, never()).save(any(Subscription.class));
    }

    @Test
    void readsSubscriptionDetailsWithoutRequestingAWriteLock() {
        Subscription subscription = new Subscription();
        subscription.setId(30L);
        subscription.setOrganization(organization);
        subscription.setCustomer(customer);
        subscription.setPack(pack);
        subscription.setStartDate(LocalDate.now(Clock.systemUTC()));
        when(subscriptionRepository.findByIdAndOrganization_Id(30L, 7L))
                .thenReturn(Optional.of(subscription));

        Subscription result = service.getSubscriptionById(30L);

        assertSame(subscription, result);
        verify(subscriptionRepository).findByIdAndOrganization_Id(30L, 7L);
        verify(subscriptionRepository, never()).findByIdForUpdate(anyLong(), anyLong());
    }
}
