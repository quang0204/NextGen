package com.wedbackendservice.nextgen.event.saga;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PaymentResultEvent {
    @Builder.Default
    String eventId = UUID.randomUUID().toString();
    String correlationId;
    @Builder.Default
    LocalDateTime occurredAt = LocalDateTime.now();

    Long orderId;
    String orderCode;
    boolean success;
    String paymentMethod;
    BigDecimal amount;
    String transactionRef;
    String message;
    List<OrderCreatedEvent.OrderItemDto> items;
}
