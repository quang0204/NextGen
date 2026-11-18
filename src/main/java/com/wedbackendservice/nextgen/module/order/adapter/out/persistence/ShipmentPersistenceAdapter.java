package com.wedbackendservice.nextgen.module.order.adapter.out.persistence;

import com.wedbackendservice.nextgen.module.order.port.out.shipment.CreateShipmentCommand;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentInfoDto;
import com.wedbackendservice.nextgen.module.order.port.out.shipment.ShipmentPort;
import com.wedbackendservice.nextgen.module.shipment.entity.Shipment;
import com.wedbackendservice.nextgen.module.shipment.enums.ShipmentStatus;
import com.wedbackendservice.nextgen.module.shipment.repository.ShipmentRepository;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShipmentPersistenceAdapter implements ShipmentPort {

  private final ShipmentRepository shipmentRepository;

  @Override
  public ShipmentInfoDto createShipment(CreateShipmentCommand command) {
    Shipment shipment =
        Shipment.builder()
            .order(command.getOrder())
            .carrierName(command.getCarrierName())
            .trackingNumber(command.getTrackingCode())
            .shippingStatus(ShipmentStatus.READY_TO_PICK.name())
            .recipientName(command.getRecipientName())
            .recipientPhone(command.getRecipientPhone())
            .shippingAddress(command.getShippingAddress())
            .codAmount(command.getCodAmount())
            .notes(command.getNotes())
            .updatedBy(command.getUpdatedBy())
            .build();
    Shipment saved = shipmentRepository.save(shipment);
    return toDto(saved);
  }

  @Override
  public Optional<ShipmentInfoDto> getShipmentByOrderId(Long orderId) {
    return shipmentRepository.findFirstByOrder_OrderId(orderId).map(this::toDto);
  }

  @Override
  public Map<Long, ShipmentInfoDto> getShipmentsByOrderIds(Collection<Long> orderIds) {
    if (orderIds == null || orderIds.isEmpty()) {
      return Collections.emptyMap();
    }
    return shipmentRepository.findByOrder_OrderIdIn(orderIds).stream()
        .filter(s -> s.getOrder() != null && s.getOrder().getOrderId() != null)
        .collect(
            Collectors.toMap(
                s -> s.getOrder().getOrderId(), this::toDto, (existing, replace) -> existing));
  }

  @Override
  public ShipmentInfoDto markShipped(
      Long orderId, String assignedShipper, String exportNote, LocalDateTime shippedAt) {
    Shipment shipment =
        shipmentRepository
            .findFirstByOrder_OrderId(orderId)
            .orElseGet(() -> Shipment.builder().build());
    shipment.setShippingStatus(ShipmentStatus.IN_TRANSIT.name());
    shipment.setShippedAt(shippedAt);
    if (assignedShipper != null) {
      shipment.setCarrierName(assignedShipper);
    }
    if (exportNote != null) {
      shipment.setNotes(exportNote);
    }
    Shipment saved = shipmentRepository.save(shipment);
    return toDto(saved);
  }

  private ShipmentInfoDto toDto(Shipment s) {
    return ShipmentInfoDto.builder()
        .shipmentId(s.getShipmentId())
        .orderId(s.getOrder() != null ? s.getOrder().getOrderId() : null)
        .trackingNumber(s.getTrackingNumber())
        .carrierName(s.getCarrierName())
        .shippingStatus(s.getShippingStatus())
        .recipientName(s.getRecipientName())
        .recipientPhone(s.getRecipientPhone())
        .shippingAddress(s.getShippingAddress())
        .codAmount(s.getCodAmount())
        .shippedAt(s.getShippedAt())
        .deliveredAt(s.getDeliveredAt())
        .build();
  }
}
