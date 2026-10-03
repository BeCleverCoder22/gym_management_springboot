package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.*;
import com.gym.management.gym_management.exception.ConflictException;
import com.gym.management.gym_management.exception.ResourceNotFoundException;
import com.gym.management.gym_management.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentRefundRepository refundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final OrganizationRepository organizationRepository;
    private final AuditService auditService;
    private final NotificationOutboxService notificationService;
    @PersistenceContext
    private EntityManager entityManager;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentRefundRepository refundRepository,
            SubscriptionRepository subscriptionRepository,
            OrganizationRepository organizationRepository,
            AuditService auditService,
            NotificationOutboxService notificationService) {
        this.paymentRepository = paymentRepository;
        this.refundRepository = refundRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.organizationRepository = organizationRepository;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<Payment> list(Pageable pageable) {
        return paymentRepository.findByOrganization_Id(TenantContext.requireOrganizationId(), pageable);
    }

    @Transactional
    public Payment create(
            Long subscriptionId, BigDecimal amount, String currency,
            PaymentMethod method, String idempotencyKey) {
        Long organizationId = TenantContext.requireOrganizationId();
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtextextended(:idempotencyKey, 0))")
                .setParameter("idempotencyKey", organizationId + ":" + idempotencyKey)
                .getSingleResult();
        return paymentRepository.findByOrganization_IdAndIdempotencyKey(organizationId, idempotencyKey)
                .map(existing -> {
                    if (!existing.getSubscription().getId().equals(subscriptionId)
                            || existing.getAmount().compareTo(amount) != 0
                            || !existing.getCurrency().equals(normalizedCurrency)
                            || existing.getMethod() != method) {
                        throw new ConflictException("La clé d'idempotence a déjà été utilisée pour une autre demande.");
                    }
                    return existing;
                })
                .orElseGet(() -> createNew(
                        organizationId, subscriptionId, amount, normalizedCurrency, method, idempotencyKey));
    }

    @Transactional
    public Payment completeCashPayment(Long paymentId) {
        Payment payment = lockPayment(paymentId);
        if (payment.getMethod() != PaymentMethod.CASH) {
            throw new ConflictException("Seul un paiement en espèces peut être confirmé manuellement.");
        }
        if (payment.getStatus() == PaymentStatus.PENDING) {
            complete(payment, "cash-" + payment.getId());
        }
        return payment;
    }

    @Transactional
    public PaymentRefund requestRefund(Long paymentId, BigDecimal amount, String reason) {
        Long organizationId = TenantContext.requireOrganizationId();
        Payment payment = lockPayment(paymentId);
        BigDecimal pendingRefunds = refundRepository.sumRequestedRefunds(paymentId, organizationId);
        if (payment.getStatus() != PaymentStatus.COMPLETED
                && payment.getStatus() != PaymentStatus.PARTIALLY_REFUNDED) {
            throw new ConflictException("Seul un paiement encaissé peut faire l'objet d'un remboursement.");
        }
        if (amount.signum() <= 0
                || payment.getRefundedAmount().add(pendingRefunds).add(amount).compareTo(payment.getAmount()) > 0) {
            throw new ConflictException("Le montant demandé dépasse le solde remboursable.");
        }
        PaymentRefund refund = refundRepository.save(new PaymentRefund(
                organizationRepository.getReferenceById(organizationId), payment, amount, reason));
        auditService.record("REFUND_REQUESTED", "PAYMENT_REFUND", refund.getId());
        return refund;
    }

    @Transactional
    public PaymentRefund completeRefund(Long refundId) {
        Long organizationId = TenantContext.requireOrganizationId();
        PaymentRefund refund = refundRepository.findByIdForUpdate(refundId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Demande de remboursement introuvable."));
        Payment payment = lockPayment(refund.getPayment().getId());
        if (refund.getStatus() != RefundStatus.REQUESTED) {
            throw new ConflictException("La demande de remboursement a déjà été traitée.");
        }
        payment.recordRefund(refund.getAmount());
        refund.complete();
        paymentRepository.save(payment);
        PaymentRefund saved = refundRepository.save(refund);
        auditService.record("REFUND_COMPLETED", "PAYMENT_REFUND", refundId);
        return saved;
    }

    @Transactional
    public Payment completeProviderPayment(Long organizationId, Long paymentId, String providerReference) {
        Payment payment = paymentRepository.findByIdForUpdate(paymentId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable."));
        if (paymentRepository.existsByOrganization_IdAndProviderReferenceAndIdNot(
                organizationId, providerReference, paymentId)) {
            throw new ConflictException("Cette référence fournisseur est déjà associée à un paiement.");
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            if (providerReference.equals(payment.getProviderReference())) {
                return payment;
            }
            throw new ConflictException("Le paiement est déjà associé à une autre référence fournisseur.");
        }
        complete(payment, providerReference);
        return payment;
    }

    private Payment createNew(
            Long organizationId, Long subscriptionId, BigDecimal amount,
            String currency, PaymentMethod method, String idempotencyKey) {
        Subscription subscription = subscriptionRepository.findByIdForUpdate(
                        subscriptionId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable."));
        Organization organization = organizationRepository.getReferenceById(organizationId);
        Payment payment = new Payment(
                organization, subscription, amount, currency, method, idempotencyKey);
        Payment saved = paymentRepository.save(payment);
        auditService.record("PAYMENT_CREATED", "PAYMENT", saved.getId());
        return saved;
    }

    private Payment lockPayment(Long id) {
        return paymentRepository.findByIdForUpdate(id, TenantContext.requireOrganizationId())
                .orElseThrow(() -> new ResourceNotFoundException("Paiement introuvable."));
    }

    private void complete(Payment payment, String providerReference) {
        payment.complete(providerReference);
        paymentRepository.save(payment);
        auditService.record("PAYMENT_COMPLETED", "PAYMENT", payment.getId());
        queueReceipt(payment);
    }

    private void queueReceipt(Payment payment) {
        notificationService.paymentCompleted(
                payment.getOrganization(),
                payment.getSubscription().getCustomer().getEmail(),
                "PAY-" + payment.getId(),
                payment.getAmount() + " " + payment.getCurrency());
    }
}
