package com.gym.management.gym_management.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(indexes = {
        @Index(name = "idx_notification_delivery", columnList = "status,next_attempt_at"),
        @Index(name = "idx_notification_org_created", columnList = "organization_id,created_at")
}, uniqueConstraints = @UniqueConstraint(
        name = "uk_notification_dedupe", columnNames = {"organization_id", "deduplication_key"}))
public class NotificationOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 40)
    private String eventType;

    @Column(name = "deduplication_key", nullable = false, length = 160)
    private String deduplicationKey;

    @Column(nullable = false, length = 254)
    private String recipient;

    @Column(nullable = false, length = 200)
    private String subject;

    @Column(nullable = false, length = 10000)
    private String body;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.QUEUED;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant nextAttemptAt;

    private Instant sentAt;

    @Column(length = 1000)
    private String lastError;

    protected NotificationOutbox() {
    }

    public NotificationOutbox(
            Organization organization, String eventType, String deduplicationKey, String recipient,
            String subject, String body) {
        this.organization = organization;
        this.eventType = eventType;
        this.deduplicationKey = deduplicationKey;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = NotificationStatus.QUEUED;
        this.attempts = 0;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        nextAttemptAt = createdAt;
    }

    public void markProcessing() {
        status = NotificationStatus.PROCESSING;
        nextAttemptAt = Instant.now().plusSeconds(300);
    }

    public void markSent() {
        status = NotificationStatus.SENT;
        sentAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
        if (attempts >= 8) {
            status = NotificationStatus.DEAD;
        } else {
            status = NotificationStatus.RETRY;
            nextAttemptAt = Instant.now().plusSeconds(Math.min(3600, 30L << Math.min(attempts, 6)));
        }
    }

    public void retryNow() {
        if (status == NotificationStatus.SENT || status == NotificationStatus.PROCESSING) {
            throw new IllegalStateException("Only terminal or queued notifications can be retried manually.");
        }
        status = NotificationStatus.RETRY;
        nextAttemptAt = Instant.now();
        lastError = null;
    }

    public Long getId() { return id; }
    public Organization getOrganization() { return organization; }
    public String getEventType() { return eventType; }
    public String getDeduplicationKey() { return deduplicationKey; }
    public String getRecipient() { return recipient; }
    public String getSubject() { return subject; }
    public String getBody() { return body; }
    public NotificationStatus getStatus() { return status; }
    public int getAttempts() { return attempts; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getNextAttemptAt() { return nextAttemptAt; }
    public Instant getSentAt() { return sentAt; }
    public String getLastError() { return lastError; }
}
