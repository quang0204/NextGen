package com.wedbackendservice.nextgen.event.saga;

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
public class StockReservedEvent {
  @Builder.Default String eventId = UUID.randomUUID().toString();
  String correlationId;
  @Builder.Default LocalDateTime occurredAt = LocalDateTime.now();

  Long orderId;
  String orderCode;
  String paymentMethod;
  BigDecimal totalAmount;
  boolean success;
  String message;
  List<OrderCreatedEvent.OrderItemDto> items;
}
