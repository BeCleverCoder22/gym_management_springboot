package com.gym.management.gym_management.service;

import com.gym.management.gym_management.entity.NotificationOutbox;
import com.gym.management.gym_management.entity.NotificationStatus;
import com.gym.management.gym_management.repository.NotificationOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class NotificationWorker {
    private static final Logger log = LoggerFactory.getLogger(NotificationWorker.class);

    private final NotificationOutboxRepository outboxRepository;
    private final NotificationDelivery delivery;
    private final NotificationProcessingService processingService;
    private final Counter sentCounter;
    private final Counter failureCounter;

    public NotificationWorker(
            NotificationOutboxRepository outboxRepository,
            org.springframework.beans.factory.ObjectProvider<NotificationDelivery> deliveryProvider,
            NotificationProcessingService processingService,
            MeterRegistry meterRegistry) {
        this.outboxRepository = outboxRepository;
        this.delivery = deliveryProvider.getIfAvailable();
        this.processingService = processingService;
        this.sentCounter = Counter.builder("gym.notifications.sent")
                .description("Number of notification messages successfully delivered")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("gym.notifications.failed")
                .description("Number of notification delivery attempts that failed")
                .register(meterRegistry);
    }

    @Scheduled(fixedDelayString = "${app.notifications.worker-delay-ms:10000}")
    public void dispatch() {
        if (delivery == null) {
            return;
        }
        List<Long> readyIds = outboxRepository.findReady(
                        List.of(NotificationStatus.QUEUED, NotificationStatus.RETRY),
                        NotificationStatus.PROCESSING, Instant.now(), PageRequest.of(0, 20))
                .stream().map(NotificationOutbox::getId).toList();
        for (Long id : readyIds) {
            NotificationOutbox notification = processingService.claim(id);
            if (notification == null) {
                continue;
            }
            try {
                delivery.send(notification.getRecipient(), notification.getSubject(), notification.getBody());
                processingService.markSent(id);
                sentCounter.increment();
            } catch (RuntimeException exception) {
                processingService.markFailed(id, exception.getClass().getSimpleName());
                failureCounter.increment();
                log.warn("Notification delivery failed: id={}, type={}, failure={}",
                        id, notification.getEventType(), exception.getClass().getSimpleName());
            }
        }
    }

}
