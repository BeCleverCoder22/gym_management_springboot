package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.Customer;
import com.gym.management.gym_management.entity.Organization;
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
import com.gym.management.gym_management.repository.OrganizationRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
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
    private final OrganizationRepository organizationRepository;
    private final Clock clock;
    private final NotificationOutboxService notificationService;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            CustomerRepository customerRepository,
            PackRepository packRepository,
            SubscriptionArchiveRepository subscriptionArchiveRepository,
            AuditService auditService,
            OrganizationRepository organizationRepository,
            Clock clock,
            NotificationOutboxService notificationService) {
        this.subscriptionRepository = subscriptionRepository;
        this.customerRepository = customerRepository;
        this.packRepository = packRepository;
        this.subscriptionArchiveRepository = subscriptionArchiveRepository;
        this.auditService = auditService;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Subscription> getAllSubscriptions(Pageable pageable) {
        return subscriptionRepository.findByOrganization_Id(TenantContext.requireOrganizationId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Subscription getSubscriptionById(Long id) {
        return subscriptionRepository.findByIdAndOrganization_Id(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
    }

    @Override
    @Transactional
    public Subscription addSubscription(Long customerId, Long packId, LocalDate startDate) {
        Customer customer = lockCustomer(customerId);
        Pack pack = getActivePack(packId);
        Organization organization = organizationRepository.getReferenceById(
                TenantContext.requireOrganizationId());
        if (!organization.getId().equals(customer.getOrganization().getId())
                || !organization.getId().equals(pack.getOrganization().getId())) {
            throw new ResourceNotFoundException("Client ou offre introuvable dans cette organisation.");
        }
        Subscription subscription = new Subscription();
        subscription.setOrganization(organization);
        subscription.setCustomer(customer);
        subscription.setPack(pack);
        subscription.setStartDate(startDate);
        subscription.setStatus(resolveStatus(startDate, subscription.getEndDate(), LocalDate.now(clock)));
        ensureNoOverlap(customer.getId(), null, startDate, subscription.getEndDate());

        Subscription saved = subscriptionRepository.save(subscription);
        updateCustomerSubscriptionFlag(customer);
        auditService.record("SUBSCRIPTION_CREATED", "SUBSCRIPTION", saved.getId());
        notificationService.subscriptionCreated(saved);
        return saved;
    }

    @Override
    @Transactional
    public Subscription updateSubscription(
            Long id, Long customerId, Long packId, LocalDate startDate) {
        Long organizationId = TenantContext.requireOrganizationId();
        Subscription subscription = subscriptionRepository.findByIdForUpdate(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
        if (subscription.getStatus() != SubscriptionStatus.SCHEDULED) {
            throw new ConflictException("Seuls les abonnements planifiés peuvent être modifiés.");
        }

        Customer previousCustomer = subscription.getCustomer();
        Customer customer = lockCustomer(customerId);
        Pack pack = getActivePack(packId);
        if (!organizationId.equals(customer.getOrganization().getId())
                || !organizationId.equals(pack.getOrganization().getId())) {
            throw new ResourceNotFoundException("Client ou offre introuvable dans cette organisation.");
        }
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
        Subscription previous = subscriptionRepository.findByIdForUpdate(
                        subscriptionId, TenantContext.requireOrganizationId())
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
        if (!customerRepository.existsByIdAndOrganization_IdAndEnabledTrue(
                customerId, TenantContext.requireOrganizationId())) {
            throw new ResourceNotFoundException("Client introuvable.");
        }
        return subscriptionRepository.findByCustomerIdAndOrganization_Id(
                customerId, TenantContext.requireOrganizationId(), pageable);
    }

    @Override
    @Transactional
    public void deleteSubscription(Long subscriptionId) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(
                        subscriptionId, TenantContext.requireOrganizationId())
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
            auditService.recordSystem(subscription.getOrganization().getId(),
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        for (Subscription subscription : subscriptionRepository
                .findByStatusAndEndDateBefore(SubscriptionStatus.ACTIVE, today)) {
            subscription.setStatus(SubscriptionStatus.EXPIRED);
            toUpdate.add(subscription);
            affectedCustomerIds.add(subscription.getCustomer().getId());
            notificationService.subscriptionExpired(subscription);
            auditService.recordSystem(subscription.getOrganization().getId(),
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        for (Subscription subscription : subscriptionRepository.findByStatusIsNull()) {
            SubscriptionStatus resolved = resolveStatus(
                    subscription.getStartDate(), subscription.getEndDate(), today);
            subscription.setStatus(resolved);
            toUpdate.add(subscription);
            affectedCustomerIds.add(subscription.getCustomer().getId());
            auditService.recordSystem(subscription.getOrganization().getId(),
                    "SUBSCRIPTION_STATUS_CHANGED", "SUBSCRIPTION", subscription.getId());
        }

        subscriptionRepository.saveAll(toUpdate);
        for (Long customerId : affectedCustomerIds) {
            customerRepository.findById(customerId).ifPresent(this::updateCustomerSubscriptionFlag);
        }
    }

    @Scheduled(cron = "${app.subscription.expiry-reminder-cron:0 0 2 * * *}",
            zone = "${app.time-zone:UTC}")
    @Transactional
    public void enqueueExpiryReminders() {
        LocalDate reminderDate = LocalDate.now(clock).plusDays(7);
        int pageNumber = 0;
        List<Subscription> subscriptions;
        do {
            subscriptions = subscriptionRepository.findByStatusAndEndDate(
                            SubscriptionStatus.ACTIVE, reminderDate, PageRequest.of(pageNumber++, 500))
                    .getContent();
            subscriptions.forEach(notificationService::subscriptionExpiring);
        } while (subscriptions.size() == 500);
    }

    private Customer lockCustomer(Long customerId) {
        return customerRepository.findByIdForUpdate(customerId, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Client introuvable."));
    }

    private Pack getActivePack(Long packId) {
        Pack pack = packRepository.findByIdAndOrganization_Id(packId, TenantContext.requireOrganizationId())
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
                        TenantContext.requireOrganizationId(), customerId,
                        startDate, endDate, NON_TERMINAL_STATUSES)
                : subscriptionRepository.existsOverlappingSubscriptionExcludingId(
                        TenantContext.requireOrganizationId(), customerId, excludedSubscriptionId,
                        startDate, endDate, NON_TERMINAL_STATUSES);
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
                customer.getOrganization().getId(), customer.getId(),
                SubscriptionStatus.ACTIVE, LocalDate.now(clock)));
        customerRepository.save(customer);
    }
}
