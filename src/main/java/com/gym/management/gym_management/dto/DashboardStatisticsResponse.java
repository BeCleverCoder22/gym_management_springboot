package com.gym.management.gym_management.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatisticsResponse(
        long totalCustomers,
        long activeCustomers,
        long newCustomersThisMonth,
        long activeSubscriptions,
        long expiredSubscriptions,
        long expiringSubscriptionsNext30Days,
        long subscriptionsSoldThisMonth,
        BigDecimal estimatedMonthlyRevenue,
        List<PackDistributionResponse> subscriptionsByPack
) {
}
