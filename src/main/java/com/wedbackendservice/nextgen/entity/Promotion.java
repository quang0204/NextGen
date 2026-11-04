package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promotions")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Promotion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long promotionId;

    @ManyToOne(fetch = FetchType.LAZY)
    User createdBy;
    String promotionName;
    String description;
    String discountType;
    BigDecimal discountValue;
    LocalDateTime startDate;
    LocalDateTime endDate;

    String status;

    @Builder.Default
    Boolean isActive = true;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;

    public String getName() {
        return promotionName != null ? promotionName : "";
    }

    public void setName(String name) {
        this.promotionName = name;
    }

    public LocalDateTime getStartAt() {
        return startDate;
    }

    public void setStartAt(LocalDateTime startAt) {
        this.startDate = startAt;
    }

    public LocalDateTime getEndAt() {
        return endDate;
    }

    public void setEndAt(LocalDateTime endAt) {
        this.endDate = endAt;
    }

    public String resolveStatus(LocalDateTime now) {
        if (status != null && !status.isBlank()) {
            return status;
        }
        if (Boolean.FALSE.equals(isActive)) {
            return "PAUSED";
        }
        if (startDate != null && now.isBefore(startDate)) {
            return "UPCOMING";
        }
        if (endDate != null && now.isAfter(endDate)) {
            return "ENDED";
        }
        return "ACTIVE";
    }
}
