package com.wedbackendservice.nextgen.event.saga;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StockReleasedEvent {
    @Builder.Default
    String eventId = UUID.randomUUID().toString();
    String correlationId;
    @Builder.Default
    LocalDateTime occurredAt = LocalDateTime.now();

    Long orderId;
    String orderCode;
    boolean success;
    String message;
}
