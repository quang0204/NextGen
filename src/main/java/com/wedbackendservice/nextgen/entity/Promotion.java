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

    @Builder.Default
    Boolean isActive = true;

    @CreatedDate
    LocalDateTime createdAt;

    @LastModifiedDate
    LocalDateTime updatedAt;
}
