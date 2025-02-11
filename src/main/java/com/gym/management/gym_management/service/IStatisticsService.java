package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Subscription;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public interface IStatisticsService {

    public long getActiveCustomersCount();
    public double getMonthlyRevenue();
    public byte[] exportSubscriptions(LocalDate startDate, LocalDate endDate) throws IOException;
}
