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
@Table(name = "order_items")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long orderItemId;

    @ManyToOne(fetch = FetchType.LAZY)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    ProductVariant variant;

    BigDecimal price;
    Integer quantity;
    BigDecimal totalPrice;

    @CreatedDate
    LocalDateTime createdAt;
}
