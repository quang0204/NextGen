package com.wedbackendservice.nextgen.port.out.shipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentInfoDto {
  private final Long shipmentId;
  private final Long orderId;
  private final String trackingNumber;
  private final String carrierName;
  private final String shippingStatus;
  private final String recipientName;
  private final String recipientPhone;
  private final String shippingAddress;
  private final BigDecimal codAmount;
  private final LocalDateTime shippedAt;
  private final LocalDateTime deliveredAt;
}
