package com.gym.management.gym_management.service;

import com.gym.management.gym_management.dto.DashboardStatisticsResponse;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionArchiveRepository archiveRepository;

    @Test
    void dashboardCombinesKpisAndUsesCurrentDateForBoundedQueries() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);
        StatisticsService service = new StatisticsService(
                customerRepository, subscriptionRepository, archiveRepository, clock);
        when(customerRepository.count()).thenReturn(18L);
        when(customerRepository.countByRegistrationDateBetween(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 29))).thenReturn(3L);
        when(subscriptionRepository.countActiveCustomersAsOf(
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 9, 29))).thenReturn(12L);
        when(subscriptionRepository.countActiveAsOf(
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 9, 29))).thenReturn(12L);
        when(subscriptionRepository.countExpiredAsOf(
                SubscriptionStatus.CANCELLED, LocalDate.of(2026, 9, 29))).thenReturn(4L);
        when(subscriptionRepository.countExpiringBetween(
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 9, 29),
                LocalDate.of(2026, 10, 29))).thenReturn(2L);
        when(subscriptionRepository.countSoldBetween(
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 29),
                SubscriptionStatus.CANCELLED)).thenReturn(5L);
        when(subscriptionRepository.calculateMonthlyRevenue(
                LocalDate.of(2026, 9, 29), SubscriptionStatus.ACTIVE))
                .thenReturn(new BigDecimal("598.80"));
        when(subscriptionRepository.countSubscriptionsByPack(SubscriptionStatus.CANCELLED))
                .thenReturn(List.<Object[]>of(new Object[]{"Annual", 8L}));

        DashboardStatisticsResponse dashboard = service.getDashboard();

        assertEquals(18, dashboard.totalCustomers());
        assertEquals(12, dashboard.activeCustomers());
        assertEquals(3, dashboard.newCustomersThisMonth());
        assertEquals(4, dashboard.expiredSubscriptions());
        assertEquals(2, dashboard.expiringSubscriptionsNext30Days());
        assertEquals(new BigDecimal("598.80"), dashboard.estimatedMonthlyRevenue());
        assertEquals("Annual", dashboard.subscriptionsByPack().getFirst().packName());
    }
}
