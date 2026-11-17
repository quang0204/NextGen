package com.wedbackendservice.nextgen.entity;

import jakarta.persistence.*;
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
@Table(name = "cart_items")
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CartItem {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long cartItemId;

  @ManyToOne(fetch = FetchType.LAZY)
  Cart cart;

  @ManyToOne(fetch = FetchType.LAZY)
  ProductVariant variant;

  Integer quantity;

  @CreatedDate LocalDateTime createdAt;

  @LastModifiedDate LocalDateTime updatedAt;
}
