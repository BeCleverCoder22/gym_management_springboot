package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.entity.NotificationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    @Query("""
            SELECT n FROM NotificationOutbox n
            WHERE (n.status IN :readyStatuses AND n.nextAttemptAt <= :now)
               OR (n.status = :processing AND n.nextAttemptAt <= :now)
            ORDER BY n.createdAt
            """)
    List<NotificationOutbox> findReady(
            @Param("readyStatuses") List<NotificationStatus> readyStatuses,
            @Param("processing") NotificationStatus processing,
            @Param("now") Instant now,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificationOutbox n WHERE n.id = :id")
    Optional<NotificationOutbox> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT n FROM NotificationOutbox n
            WHERE n.id = :id AND n.organization.id = :organizationId
            """)
    Optional<NotificationOutbox> findByIdAndOrganizationForUpdate(
            @Param("id") Long id, @Param("organizationId") Long organizationId);

    Page<NotificationOutbox> findByOrganization_Id(
            Long organizationId, Pageable pageable);
}
