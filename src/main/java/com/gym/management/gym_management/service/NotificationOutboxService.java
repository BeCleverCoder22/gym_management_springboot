package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.entity.Organization;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.repository.NotificationOutboxRepository;
import com.gym.management.gym_management.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxService {
    private final NotificationOutboxRepository outboxRepository;
    private final OrganizationRepository organizationRepository;

    public NotificationOutboxService(
            NotificationOutboxRepository outboxRepository,
            OrganizationRepository organizationRepository) {
        this.outboxRepository = outboxRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void welcomeOrganization(Organization organization, String administratorEmail) {
        enqueue(organization, "WELCOME", "organization-welcome:" + organization.getId(),
                administratorEmail,
                "Bienvenue sur Gym Management",
                "Votre espace " + organization.getName() + " est prêt.");
    }

    @Transactional
    public void subscriptionCreated(Subscription subscription) {
        Customer customer = subscription.getCustomer();
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }
        enqueue(subscription.getOrganization(), "SUBSCRIPTION_CREATED",
                "subscription-created:" + subscription.getId(), customer.getEmail(),
                "Confirmation de votre abonnement",
                "Votre abonnement " + subscription.getOfferNameAtPurchase()
                        + " commence le " + subscription.getStartDate()
                        + " et se termine le " + subscription.getEndDate() + ".");
    }

    @Transactional
    public void subscriptionExpiring(Subscription subscription) {
        Customer customer = subscription.getCustomer();
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }
        enqueue(subscription.getOrganization(), "SUBSCRIPTION_EXPIRING",
                "subscription-expiring:" + subscription.getId(), customer.getEmail(),
                "Votre abonnement arrive à échéance",
                "Votre abonnement se termine le " + subscription.getEndDate() + ".");
    }

    @Transactional
    public void subscriptionExpired(Subscription subscription) {
        Customer customer = subscription.getCustomer();
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            return;
        }
        enqueue(subscription.getOrganization(), "SUBSCRIPTION_EXPIRED",
                "subscription-expired:" + subscription.getId(), customer.getEmail(),
                "Votre abonnement est arrivé à échéance",
                "Votre abonnement s'est terminé le " + subscription.getEndDate() + ".");
    }

    @Transactional
    public void paymentCompleted(
            Organization organization, String customerEmail, String receiptNumber, String amount) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return;
        }
        enqueue(organization, "PAYMENT_COMPLETED", "payment-receipt:" + receiptNumber, customerEmail,
                "Confirmation de paiement",
                "Paiement confirmé. Référence du reçu : " + receiptNumber + ", montant : " + amount + ".");
    }

    private void enqueue(
            Organization organization, String type, String deduplicationKey,
            String recipient, String subject, String body) {
        Organization managedOrganization = organization.getId() == null
                ? organization
                : organizationRepository.getReferenceById(organization.getId());
        outboxRepository.save(new NotificationOutbox(
                managedOrganization, type, deduplicationKey, recipient, subject, body));
    }
}
