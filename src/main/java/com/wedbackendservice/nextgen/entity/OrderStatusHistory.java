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
@Table(name = "order_status_history")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    User changedBy;

    String previousStatus;
    String newStatus;

    String note;

    @CreatedDate
    LocalDateTime createdAt;

    public String getFromStatus() {
        return previousStatus;
    }

    public String getToStatus() {
        return newStatus;
    }

    public String getReason() {
        return note;
    }
}
