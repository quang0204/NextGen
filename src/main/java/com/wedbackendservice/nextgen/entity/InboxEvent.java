package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "inbox_events",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uk_inbox_event_consumer",
          columnNames = {"eventId", "consumerGroup"})
    },
    indexes = {@Index(name = "idx_inbox_event_id", columnList = "eventId")})
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InboxEvent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  @Column(nullable = false)
  String eventId;

  @Column(nullable = false)
  String consumerGroup;

  String eventType;

  String aggregateId;

  @CreatedDate LocalDateTime processedAt;
}
