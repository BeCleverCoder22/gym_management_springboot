package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.SubscriptionArchive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface SubscriptionArchiveRepository extends JpaRepository<SubscriptionArchive, Long> {
    @Query("SELECT sa FROM SubscriptionArchive sa WHERE sa.startDate BETWEEN :startDate AND :endDate")
    List<SubscriptionArchive> findSubscriptionsArchiveForPeriod(LocalDate startDate, LocalDate endDate);
}
