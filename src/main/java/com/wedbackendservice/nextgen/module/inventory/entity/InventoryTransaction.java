package com.wedbackendservice.nextgen.module.inventory.entity;

import com.wedbackendservice.nextgen.module.order.entity.Order;
import com.wedbackendservice.nextgen.module.product.entity.ProductVariant;
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
@Table(name = "inventory_transactions")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InventoryTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long txnId;

  @ManyToOne(fetch = FetchType.LAZY)
  ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY)
  Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  User performedBy;

  String transactionType;
  String formCode;
  Integer quantity;

  @Column(name = "qty_before")
  Integer qtyBefore;

  @Column(name = "qty_after")
  Integer qtyAfter;

  String reason;

  @CreatedDate LocalDateTime createdAt;
}
