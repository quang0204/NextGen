package com.wedbackendservice.nextgen.port.out.shipment;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

public interface ShipmentPort {
  ShipmentInfoDto createShipment(CreateShipmentCommand command);

  Optional<ShipmentInfoDto> getShipmentByOrderId(Long orderId);

  Map<Long, ShipmentInfoDto> getShipmentsByOrderIds(Collection<Long> orderIds);

  ShipmentInfoDto markShipped(
      Long orderId, String assignedShipper, String exportNote, LocalDateTime shippedAt);
}
