package com.wedbackendservice.nextgen.port.out.shipment;

import com.wedbackendservice.nextgen.entity.Order;
import com.wedbackendservice.nextgen.entity.User;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateShipmentCommand {
  private final Order order;
  private final String carrierName;
  private final String trackingCode;
  private final String recipientName;
  private final String recipientPhone;
  private final String shippingAddress;
  private final BigDecimal codAmount;
  private final String notes;
  private final User updatedBy;
}
