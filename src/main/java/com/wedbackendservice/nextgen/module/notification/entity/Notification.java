package com.wedbackendservice.nextgen.module.notification.entity;

import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.user.entity.User;
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

  @Builder.Default Boolean isRead = false;

  @CreatedDate LocalDateTime createdAt;
}
