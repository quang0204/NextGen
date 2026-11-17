package com.wedbackendservice.nextgen.scheduler;

import com.wedbackendservice.nextgen.entity.OutboxEvent;
import com.wedbackendservice.nextgen.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Scheduled(fixedDelay = 1500)
    @Transactional
    public void publishPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository.findByStatusOrderByCreatedAtAsc("PENDING", PageRequest.of(0, 50));
        if (pendingEvents.isEmpty()) {
            return;
        }

        for (OutboxEvent event : pendingEvents) {
            try {
                kafkaTemplate.send(event.getTopic(), event.getAggregateId(), event.getPayload())
                        .get(5, TimeUnit.SECONDS);

                event.setStatus("SUCCESS");
                event.setProcessedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("[OutboxPublisher] Published event ID: {} ({}) to topic: {}",
                        event.getId(), event.getEventType(), event.getTopic());
            } catch (Exception e) {
                int retries = event.getRetryCount() != null ? event.getRetryCount() + 1 : 1;
                event.setRetryCount(retries);
                event.setLastError(e.getMessage());
                if (retries >= 5) {
                    event.setStatus("FAILED");
                    log.error("[OutboxPublisher] Event ID {} failed permanently after {} retries: {}",
                            event.getId(), retries, e.getMessage());
                } else {
                    log.warn("[OutboxPublisher] Event ID {} retry {}/5 failed: {}",
                            event.getId(), retries, e.getMessage());
                }
                outboxEventRepository.save(event);
            }
        }
    }
}
