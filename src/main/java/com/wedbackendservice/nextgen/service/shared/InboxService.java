package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.entity.InboxEvent;
import com.wedbackendservice.nextgen.repository.InboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class InboxService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional(readOnly = true)
    public boolean isAlreadyProcessed(String eventId, String consumerGroup) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return inboxEventRepository.existsByEventIdAndConsumerGroup(eventId, consumerGroup);
    }

    @Transactional
    public void markAsProcessed(String eventId, String consumerGroup, String eventType, String aggregateId) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        try {
            inboxEventRepository.save(InboxEvent.builder()
                    .eventId(eventId)
                    .consumerGroup(consumerGroup)
                    .eventType(eventType)
                    .aggregateId(aggregateId)
                    .processedAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.debug("[InboxService] Event {} already recorded in inbox for consumer {}", eventId, consumerGroup);
        }
    }
}
