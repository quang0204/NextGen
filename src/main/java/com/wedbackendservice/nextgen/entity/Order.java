package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "orders",
    indexes = {
      @Index(name = "idx_orders_user_created", columnList = "user_user_id, created_at DESC"),
      @Index(name = "idx_orders_order_code", columnList = "order_code")
    })
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long orderId;

  @ManyToOne(fetch = FetchType.LAZY)
  User user;

  @ManyToOne(fetch = FetchType.LAZY)
  Voucher voucher;

  @Column(name = "order_code", nullable = false, unique = true, length = 64)
  String orderCode;

  BigDecimal totalAmount;
  BigDecimal discountAmount;
  BigDecimal shippingFee;
  BigDecimal finalAmount;
  String status;
  String paymentMethod;
  String paymentStatus;
  String shippingAddress;
  String recipientName;
  String recipientPhone;

  String note;
  String cancelReason;
  LocalDateTime completedAt;

  @Version Long version;

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;

  public BigDecimal getSubtotal() {
    return totalAmount != null ? totalAmount : BigDecimal.ZERO;
  }

  public BigDecimal getVoucherDiscount() {
    return discountAmount != null ? discountAmount : BigDecimal.ZERO;
  }

  public BigDecimal getFinalTotal() {
    return finalAmount != null
        ? finalAmount
        : (totalAmount != null ? totalAmount : BigDecimal.ZERO);
  }
}
