package com.wedbackendservice.nextgen.core.outbox.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.outbox.entity.OutboxEvent;
import com.wedbackendservice.nextgen.core.outbox.repository.OutboxEventRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxService {

  private final OutboxEventRepository outboxEventRepository;
  private final ObjectMapper objectMapper;

  @Transactional
  public void saveEvent(
      String aggregateType, String aggregateId, String eventType, String topic, Object payloadObj) {
    try {
      String payloadJson = objectMapper.writeValueAsString(payloadObj);
      OutboxEvent event =
          OutboxEvent.builder()
              .aggregateType(aggregateType)
              .aggregateId(aggregateId)
              .eventType(eventType)
              .topic(topic)
              .payload(payloadJson)
              .status("PENDING")
              .retryCount(0)
              .createdAt(LocalDateTime.now())
              .build();
      outboxEventRepository.save(event);
      log.info(
          "[Outbox] Saved event: {} for aggregateId: {} on topic: {}",
          eventType,
          aggregateId,
          topic);
    } catch (Exception e) {
      log.error(
          "[Outbox] Failed to serialize and save outbox event {}: {}",
          eventType,
          e.getMessage(),
          e);
      throw new RuntimeException("Lỗi lưu sự kiện Outbox: " + e.getMessage(), e);
    }
  }

  @Transactional
  public void saveProcessedEvent(
      String aggregateType, String aggregateId, String eventType, String topic, Object payloadObj) {
    try {
      String payloadJson = objectMapper.writeValueAsString(payloadObj);
      LocalDateTime now = LocalDateTime.now();
      OutboxEvent event =
          OutboxEvent.builder()
              .aggregateType(aggregateType)
              .aggregateId(aggregateId)
              .eventType(eventType)
              .topic(topic != null ? topic : "order-events-audit")
              .payload(payloadJson)
              .status("SUCCESS")
              .retryCount(0)
              .createdAt(now)
              .processedAt(now)
              .build();
      outboxEventRepository.save(event);
      log.info(
          "[Outbox] Saved completed SUCCESS event: {} for aggregateId: {}", eventType, aggregateId);
    } catch (Exception e) {
      log.warn("[Outbox] Failed to save processed event {}: {}", eventType, e.getMessage());
    }
  }
}
