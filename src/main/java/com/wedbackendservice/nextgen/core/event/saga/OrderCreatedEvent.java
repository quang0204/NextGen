package com.wedbackendservice.nextgen.core.event.saga;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderCreatedEvent {
  @Builder.Default String eventId = UUID.randomUUID().toString();
  String correlationId;
  @Builder.Default LocalDateTime occurredAt = LocalDateTime.now();

  Long orderId;
  String orderCode;
  Long userId;
  String recipientName;
  String recipientPhone;
  String shippingAddress;
  String paymentMethod;
  BigDecimal totalAmount;
  List<OrderItemDto> items;

  @Getter
  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @FieldDefaults(level = AccessLevel.PRIVATE)
  public static class OrderItemDto {
    Long variantId;
    Integer quantity;
    BigDecimal unitPrice;
  }
}
