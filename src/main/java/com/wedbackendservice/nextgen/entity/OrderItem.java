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

    @Column(name = "sku_snapshot")
    String skuSnapshot;

    @Column(name = "product_name_snapshot")
    String productNameSnapshot;

    @Column(name = "color_snapshot")
    String colorSnapshot;

    @Column(name = "rom_snapshot")
    String romSnapshot;

    @Column(name = "image_url")
    String imageUrl;

    BigDecimal price;
    Integer quantity;
    BigDecimal totalPrice;

    @CreatedDate
    LocalDateTime createdAt;

    public BigDecimal getUnitPrice() {
        return price;
    }

    public BigDecimal getLineTotal() {
        return totalPrice != null ? totalPrice : (price != null && quantity != null ? price.multiply(BigDecimal.valueOf(quantity)) : BigDecimal.ZERO);
    }
}
