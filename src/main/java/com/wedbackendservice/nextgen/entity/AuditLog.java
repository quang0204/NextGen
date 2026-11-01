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
@Table(name = "audit_logs")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    User user;

    @Column(nullable = false)
    String action;

    @Column(name = "entity_name")
    String entityName;

    @Column(name = "entity_id")
    Long entityId;

    @Column(name = "old_value")
    String oldValue;

    @Column(name = "new_value")
    String newValue;

    @CreatedDate
    @Column(name = "created_at")
    LocalDateTime createdAt;

    public Long getUserId() {
        return user != null ? user.getUserId() : null;
    }

    public Long getAuditId() {
        return logId;
    }
}
