package com.gym.management.gym_management.service;

import java.io.IOException;
import java.time.LocalDate;
import com.gym.management.gym_management.dto.DashboardStatisticsResponse;
import com.gym.management.gym_management.dto.MonthlyRevenueResponse;

public interface IStatisticsService {

    DashboardStatisticsResponse getDashboard();
    java.math.BigDecimal getMonthlyRevenue();
    public byte[] exportSubscriptions(LocalDate startDate, LocalDate endDate) throws IOException;
    java.math.BigDecimal getRevenueForPeriod(LocalDate startDate, LocalDate endDate);
    java.util.List<MonthlyRevenueResponse> getRevenueByMonth(LocalDate startDate, LocalDate endDate);
}
