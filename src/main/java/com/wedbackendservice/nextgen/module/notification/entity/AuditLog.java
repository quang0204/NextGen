package com.wedbackendservice.nextgen.module.notification.entity;

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
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long logId;

  @ManyToOne(fetch = FetchType.LAZY)
  User user;

  String action;
  String entityName;
  Long entityId;
  String oldValue;
  String newValue;

  @CreatedDate LocalDateTime createdAt;
}
