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
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    User user;

    @ManyToOne(fetch = FetchType.LAZY)
    Order order;

    String title;
    String content;

    @Builder.Default
    Boolean isRead = false;

    @CreatedDate
    LocalDateTime createdAt;
}
