package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SubscriptionService implements ISubscriptionService{
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Override
    public List<Subscription> getAllSubscriptions() {
        return subscriptionRepository.findAll(); // Récupérer tous les abonnements
    }

    @Override
    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id) // Récupérer un abonnement
                .orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'ID : " + id));
    }

    @Override
    public Subscription addSubscription(Subscription subscription) {
        return subscriptionRepository.save(subscription); // Ajouter un abonnement
    }

    @Override
    public Subscription updateSubscription(Long id, Subscription updatedSubscription) {
        return subscriptionRepository.findById(id).map(subscription -> {
            if (updatedSubscription.getStartDate() != null) {
                subscription.setStartDate(updatedSubscription.getStartDate());
            }
            if (updatedSubscription.getCustomer() != null) {
                subscription.setCustomer(updatedSubscription.getCustomer());
            }
            if (updatedSubscription.getPack() != null) {
                subscription.setPack(updatedSubscription.getPack());
            }
            return subscriptionRepository.save(subscription);
        }).orElseThrow(() -> new RuntimeException("Abonnement non trouvé avec l'ID : " + id));
    }


    @Override
    public List<Subscription> getSubscriptionsByCustomerId(Long customerId) {
        return subscriptionRepository.findAll().stream()
                .filter(sub -> sub.getCustomer().getId().equals(customerId))
                .toList(); // Récupérer les abonnements d'un client
    }

    @Override
    public void deleteSubscription(Long id) {
        subscriptionRepository.deleteById(id); // Supprimer un abonnement
    }
}
