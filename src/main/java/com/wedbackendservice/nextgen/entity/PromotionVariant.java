package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promotion_variants")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionVariant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long promoVariantId;

    @ManyToOne(fetch = FetchType.LAZY)
    Promotion promotion;
    @ManyToOne(fetch = FetchType.LAZY)
    ProductVariant variant;
    BigDecimal promotionalPrice;
    @CreatedDate
    LocalDateTime createdAt;
}
