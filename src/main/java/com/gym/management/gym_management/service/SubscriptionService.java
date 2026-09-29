package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Pack;
import com.gym.management.gym_management.entity.Subscription;
import com.gym.management.gym_management.entity.SubscriptionArchive;
import com.gym.management.gym_management.entity.SubscriptionStatus;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.CustomerRepository;
import com.gym.management.gym_management.repository.PackRepository;
import com.gym.management.gym_management.repository.SubscriptionArchiveRepository;
import com.gym.management.gym_management.repository.SubscriptionRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SubscriptionService implements ISubscriptionService {
    private static final Set<SubscriptionStatus> NON_TERMINAL_STATUSES =
            Set.of(SubscriptionStatus.SCHEDULED, SubscriptionStatus.ACTIVE);

    private final SubscriptionRepository subscriptionRepository;
    private final CustomerRepository customerRepository;
    private final PackRepository packRepository;
    private final SubscriptionArchiveRepository subscriptionArchiveRepository;
    private final AuditService auditService;
    private final Clock clock;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            CustomerRepository customerRepository,
            PackRepository packRepository,
            SubscriptionArchiveRepository subscriptionArchiveRepository,
            AuditService auditService,
            Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerRepository = customerRepository;
        this.packRepository = packRepository;
        this.subscriptionArchiveRepository = subscriptionArchiveRepository;
        this.auditService = auditService;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
    }

    @Override
    @Transactional
    public Subscription addSubscription(Long customerId, Long packId, LocalDate startDate) {
        Customer customer = lockCustomer(customerId);
        Pack pack = getActivePack(packId);
        Subscription subscription = new Subscription();
        subscription.setCustomer(customer);
        subscription.setPack(pack);
        subscription.setStartDate(startDate);
        subscription.setStatus(resolveStatus(startDate, subscription.getEndDate(), LocalDate.now(clock)));
        ensureNoOverlap(customer.getId(), null, startDate, subscription.getEndDate());

        Subscription saved = subscriptionRepository.save(subscription);
        updateCustomerSubscriptionFlag(customer);
        auditService.record("SUBSCRIPTION_CREATED", "SUBSCRIPTION", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Subscription updateSubscription(
            Long id, Long customerId, Long packId, LocalDate startDate) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
        if (subscription.getStatus() != SubscriptionStatus.SCHEDULED) {
            throw new ConflictException("Seuls les abonnements planifiés peuvent être modifiés.");
        }

        Customer previousCustomer = subscription.getCustomer();
        Customer customer = lockCustomer(customerId);
        Pack pack = getActivePack(packId);
        subscription.setCustomer(customer);
        subscription.setPack(pack);
        subscription.setStartDate(startDate);
        subscription.setStatus(resolveStatus(startDate, subscription.getEndDate(), LocalDate.now(clock)));
        ensureNoOverlap(customer.getId(), id, startDate, subscription.getEndDate());

        Subscription saved = subscriptionRepository.save(subscription);
        updateCustomerSubscriptionFlag(previousCustomer);
        updateCustomerSubscriptionFlag(customer);
        auditService.record("SUBSCRIPTION_UPDATED", "SUBSCRIPTION", saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public Subscription renewSubscription(Long subscriptionId) {
        Subscription previous = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
        LocalDate nextStart = previous.getEndDate().plusDays(1);
        if (nextStart.isBefore(LocalDate.now(clock))) {
            nextStart = LocalDate.now(clock);
        }
        return addSubscription(
                previous.getCustomer().getId(), previous.getPack().getId(), nextStart);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Subscription> getSubscriptionsByCustomerId(Long customerId, Pageable pageable) {
        if (!customerRepository.existsById(customerId)) {
            throw new ResourceNotFoundException("Client introuvable.");
        }
        return subscriptionRepository.findByCustomerId(customerId, pageable);
    }

    @Override
    @Transactional
    public void deleteSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(subscriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
        if (subscription.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new ConflictException("Cet abonnement est déjà annulé.");
        }

        SubscriptionArchive archive = new SubscriptionArchive();
        archive.archiveSubscription(subscription);
        subscriptionArchiveRepository.save(archive);

        Customer customer = subscription.getCustomer();
        subscription.setStatus(SubscriptionStatus.CANCELLED);
        subscriptionRepository.save(subscription);
        updateCustomerSubscriptionFlag(customer);
        auditService.record("SUBSCRIPTION_CANCELLED", "SUBSCRIPTION", subscriptionId);
    }

    @Scheduled(cron = "${app.subscription.reconciliation-cron:0 0 1 * * *}",
            zone = "${app.time-zone:UTC}")
    @Transactional
    public void reconcileSubscriptionStatuses() {
        LocalDate today = LocalDate.now(clock);
        List<Subscription> toUpdate = new ArrayList<>();
        Set<Long> affectedCustomerIds = new HashSet<>();

        for (Subscription subscription : subscriptionRepository
                .findByStatusAndStartDateLessThanEqual(SubscriptionStatus.SCHEDULED, today)) {
            subscription.setStatus(resolveStatus(
                    subscription.getStartDate(), subscription.getEndDate(), today));
            toUpdate.add(subscription);
            affectedCustomerIds.add(subscription.getCustomer().getId());
            auditService.recordSystem(
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        for (Subscription subscription : subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today)) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            toUpdate.add(subscription);
            affectedCustomerIds.add(subscription.getCustomer().getId());
            auditService.recordSystem(
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        for (Subscription subscription : subscriptionRepository.findByStatusIsNull()) {
            SubscriptionStatus resolved = resolveStatus(
                    subscription.getStartDate(), subscription.getEndDate(), today);
            subscription.setStatus(resolved);
            toUpdate.add(subscription);
            affectedCustomerIds.add(subscription.getCustomer().getId());
            auditService.recordSystem(
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        subscriptionRepository.saveAll(toUpdate);
        for (Long customerId : affectedCustomerIds) {
            customerRepository.findById(customerId).ifPresent(this::updateCustomerSubscriptionFlag);
        }
    }

    private Customer lockCustomer(Long customerId) {
        return customerRepository.findByIdForUpdate(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable."));
    }

    private Pack getActivePack(Long packId) {
        Pack pack = packRepository.findById(packId)
                .orElseThrow(() -> new ResourceNotFoundException("Offre introuvable."));
        if (Boolean.FALSE.equals(pack.getActive())) {
            throw new ConflictException("Cette offre n'est plus disponible.");
        }
        return pack;
    }

    private void ensureNoOverlap(
            Long customerId, Long excludedSubscriptionId, LocalDate startDate, LocalDate endDate) {
        boolean overlaps = excludedSubscriptionId == null
                ? subscriptionRepository.existsOverlappingSubscription(
                        customerId, startDate, endDate, NON_TERMINAL_STATUSES)
                : subscriptionRepository.existsOverlappingSubscriptionExcludingId(
                        customerId, excludedSubscriptionId, startDate, endDate, NON_TERMINAL_STATUSES);
        if (overlaps) {
            throw new ConflictException(
                    "Le client possède déjà un abonnement planifié ou actif sur cette période.");
        }
    }

    private SubscriptionStatus resolveStatus(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (endDate.isBefore(today)) {
            return SubscriptionStatus.EXPIRED;
        }
        return startDate.isAfter(today) ? SubscriptionStatus.SCHEDULED : SubscriptionStatus.ACTIVE;
    }

    private void updateCustomerSubscriptionFlag(Customer customer) {
        customer.setActiveSubscription(!Boolean.FALSE.equals(customer.getEnabled())
                && subscriptionRepository.hasActiveSubscription(
                customer.getId(), SubscriptionStatus.ACTIVE, LocalDate.now(clock)));
        customerRepository.save(customer);
    }
}
