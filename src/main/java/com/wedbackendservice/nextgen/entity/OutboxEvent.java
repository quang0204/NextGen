package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status, created_at ASC")
})
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OutboxEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String aggregateType;
    String aggregateId;
    String eventType;
    String topic;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    String payload;

    String status; // PENDING, PROCESSED, FAILED
    Integer retryCount;
    String lastError;

    @CreatedDate
    LocalDateTime createdAt;
    LocalDateTime processedAt;
}
