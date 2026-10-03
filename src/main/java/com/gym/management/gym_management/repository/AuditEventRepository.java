package com.gym.management.gym_management.repository;

import com.gym.management.gym_management.entity.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    org.springframework.data.domain.Page<AuditEvent> findByOrganization_Id(
            Long organizationId, org.springframework.data.domain.Pageable pageable);
}
