package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "inventories")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Inventory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long inventoryId;

    @OneToOne(fetch = FetchType.LAZY)
    ProductVariant variant;

    @Builder.Default
    Integer quantity = 0;

    @Builder.Default
    Integer reservedQuantity = 0;

    @Builder.Default
    @Column(name = "min_threshold")
    Integer minThreshold = 5;

    @LastModifiedDate
    LocalDateTime updatedAt;

    public Integer getOnHandQty() {
        return quantity != null ? quantity : 0;
    }

    public Integer getReservedQty() {
        return reservedQuantity != null ? reservedQuantity : 0;
    }

    public Integer getAvailableQty() {
        return Math.max(0, getOnHandQty() - getReservedQty());
    }

    public Integer getMinThreshold() {
        return minThreshold != null ? minThreshold : 5;
    }
}
