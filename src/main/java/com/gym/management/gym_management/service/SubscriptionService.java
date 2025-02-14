package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionArchive;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class SubscriptionService implements ISubscriptionService{
    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private SubscriptionArchiveRepository subscriptionArchiveRepository;

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

    // 🔹 Exécuter cette tâche tous les jours à minuit (00:00)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void activateSubscriptions() {
        LocalDate today = LocalDate.now();
        List<Subscription> subscriptions = subscriptionRepository.findByStartDate(today);

        for (Subscription subscription : subscriptions) {
            Customer customer = subscription.getCustomer();
            if (!customer.isActiveSubscription()) { // Évite de mettre à jour inutilement
                customer.setActiveSubscription(true);
                customerRepository.save(customer);
                System.out.println("Abonnement activé pour : " + customer.getFirstName() + " " + customer.getLastName());
            }
        }
    }

    @Override
    public void deleteSubscription(Long subscriptionId) {
        // Récupérer l'abonnement à supprimer
        Subscription subscription = subscriptionRepository.findById(subscriptionId).orElseThrow(() -> new RuntimeException("Subscription not found"));

        // Archiver l'abonnement
        SubscriptionArchive subscriptionArchive = new SubscriptionArchive();
        subscriptionArchive.archiveSubscription(subscription);

        // Sauvegarder l'abonnement dans la table d'archives
        subscriptionArchiveRepository.save(subscriptionArchive);

        // Supprimer l'abonnement de la table principale
        subscriptionRepository.delete(subscription);
    }
}
