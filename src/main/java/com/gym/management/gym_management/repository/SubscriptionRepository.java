package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Query("SELECT SUM(p.monthlyPrice) FROM Subscription s JOIN s.pack p WHERE s.startDate <= CURRENT_DATE")
    Double calculateMonthlyRevenue();

    @Query("SELECT s FROM Subscription s WHERE s.startDate BETWEEN :startDate AND :endDate")
    List<Subscription> findSubscriptionsForPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
