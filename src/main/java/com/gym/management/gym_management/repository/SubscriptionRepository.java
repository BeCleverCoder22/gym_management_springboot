package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @Override
    @EntityGraph(attributePaths = {"customer", "pack"})
    Page<Subscription> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"customer", "pack"})
    List<Subscription> findAll();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"customer", "pack"})
    @Query("SELECT s FROM Subscription s WHERE s.id = :id")
    Optional<Subscription> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"customer", "pack"})
    Page<Subscription> findByCustomerId(Long customerId, Pageable pageable);

    @EntityGraph(attributePaths = {"customer", "pack"})
    List<Subscription> findByStatusAndStartDateLessThanEqual(
            SubscriptionStatus status, LocalDate startDate);

    @EntityGraph(attributePaths = {"customer", "pack"})
    List<Subscription> findByStatusAndEndDateBefore(
            SubscriptionStatus status, LocalDate endDate);

    @EntityGraph(attributePaths = {"customer", "pack"})
    List<Subscription> findByStatusIsNull();

    @Query("""
            SELECT SUM(COALESCE(s.monthlyPriceAtPurchase, p.monthlyPrice))
            FROM Subscription s JOIN s.pack p
            WHERE s.startDate <= :today
              AND (s.endDate >= :today OR s.endDate IS NULL)
              AND (s.status = :active OR s.status IS NULL)
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
            """)
    java.math.BigDecimal calculateMonthlyRevenue(
            @Param("today") LocalDate today,
            @Param("active") SubscriptionStatus active);

    @Query("""
            SELECT s FROM Subscription s
            WHERE s.startDate BETWEEN :startDate AND :endDate
              AND (s.status IS NULL OR s.status <> com.gym.management.gym_management.entity.SubscriptionStatus.CANCELLED)
            """)
    List<Subscription> findSubscriptionsForPeriod(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Subscription s
            WHERE s.customer.id = :customerId
              AND s.status IN :statuses
              AND s.startDate <= :endDate
              AND s.endDate >= :startDate
            """)
    boolean existsOverlappingSubscription(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Set<SubscriptionStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Subscription s
            WHERE s.customer.id = :customerId
              AND s.id <> :subscriptionId
              AND s.status IN :statuses
              AND s.startDate <= :endDate
              AND s.endDate >= :startDate
            """)
    boolean existsOverlappingSubscriptionExcludingId(
            @Param("customerId") Long customerId,
            @Param("subscriptionId") Long subscriptionId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") Set<SubscriptionStatus> statuses);

    @Query("""
            SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
            FROM Subscription s
            WHERE s.customer.id = :customerId
              AND s.status = :active
              AND s.startDate <= :today
              AND s.endDate >= :today
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
            """)
    boolean hasActiveSubscription(
            @Param("customerId") Long customerId,
            @Param("active") SubscriptionStatus active,
            @Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(DISTINCT s.customer.id) FROM Subscription s
            WHERE (s.status = :active OR s.status IS NULL)
              AND s.startDate <= :today
              AND (s.endDate >= :today OR s.endDate IS NULL)
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
            """)
    long countActiveCustomersAsOf(
            @Param("active") SubscriptionStatus active, @Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE (s.status = :active OR s.status IS NULL)
              AND s.startDate <= :today
              AND (s.endDate >= :today OR s.endDate IS NULL)
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
            """)
    long countActiveAsOf(
            @Param("active") SubscriptionStatus active, @Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE s.status <> :cancelled AND s.endDate < :today
            """)
    long countExpiredAsOf(
            @Param("cancelled") SubscriptionStatus cancelled, @Param("today") LocalDate today);

    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE (s.status = :active OR s.status IS NULL)
              AND s.endDate BETWEEN :startDate AND :endDate
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
              AND (s.customer.enabled IS NULL OR s.customer.enabled = true)
            """)
    long countExpiringBetween(
            @Param("active") SubscriptionStatus active,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            SELECT COUNT(s) FROM Subscription s
            WHERE s.startDate BETWEEN :startDate AND :endDate
              AND (s.status IS NULL OR s.status <> :cancelled)
            """)
    long countSoldBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cancelled") SubscriptionStatus cancelled);

    @Query("""
            SELECT SUM(COALESCE(s.monthlyPriceAtPurchase, p.monthlyPrice))
            FROM Subscription s JOIN s.pack p
            WHERE s.startDate BETWEEN :startDate AND :endDate
              AND (s.status IS NULL OR s.status <> :cancelled)
            """)
    java.math.BigDecimal sumMonthlyValueStartedBetween(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("cancelled") SubscriptionStatus cancelled);

    @Query("""
            SELECT COALESCE(s.offerNameAtPurchase, p.offerName), COUNT(s)
            FROM Subscription s JOIN s.pack p
            WHERE s.status IS NULL OR s.status <> :cancelled
            GROUP BY COALESCE(s.offerNameAtPurchase, p.offerName)
            ORDER BY COUNT(s) DESC
            """)
    List<Object[]> countSubscriptionsByPack(
            @Param("cancelled") SubscriptionStatus cancelled);

    @Query(value = """
            SELECT date_trunc('month', s.start_date)::date AS month,
                   SUM(COALESCE(s.monthly_price_at_purchase, p.monthly_price)) AS monthly_value
            FROM subscription s
            JOIN pack p ON p.id = s.pack_id
            WHERE s.start_date BETWEEN :startDate AND :endDate
              AND (s.status IS NULL OR s.status <> 'CANCELLED')
            GROUP BY date_trunc('month', s.start_date)
            ORDER BY month
            """, nativeQuery = true)
    List<Object[]> sumMonthlyValueByMonth(
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
