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
    @Column(nullable = false)
    Integer quantity = 0;

    @Builder.Default
    Integer reservedQuantity = 0;

    @LastModifiedDate
    LocalDateTime updatedAt;
}
