package com.gym.management.gym_management.service;

import java.time.LocalDate;
import com.gym.management.gym_management.entity.Subscription;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ISubscriptionService {
    public Page<Subscription> getAllSubscriptions(Pageable pageable);
    public Subscription getSubscriptionById(Long id);
    public Subscription updateSubscription(Long id, Long customerId, Long packId, LocalDate startDate);
    public Subscription addSubscription(Long customerId, Long packId, LocalDate startDate);
    public Subscription renewSubscription(Long subscriptionId);
    public Page<Subscription> getSubscriptionsByCustomerId(Long customerId, Pageable pageable);
    public void deleteSubscription(Long id);
}
