package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Subscription;

import java.util.List;

public interface ISubscriptionService {
    public List<Subscription> getAllSubscriptions();
    public Subscription getSubscriptionById(Long id);
    public Subscription updateSubscription(Long id, Subscription updatedSubscription);
    public Subscription addSubscription(Subscription subscription);
    public List<Subscription> getSubscriptionsByCustomerId(Long customerId);
    public void deleteSubscription(Long id);
}
