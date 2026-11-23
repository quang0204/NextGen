package com.wedbackendservice.nextgen.module.shipment.mapper;

import com.wedbackendservice.nextgen.module.shipment.dto.portal.response.AdminShipmentItemResponse;
import com.wedbackendservice.nextgen.module.shipment.entity.Shipment;
import com.wedbackendservice.nextgen.module.user.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
  @Mapping(source = "shipment.order.orderId", target = "orderId")
  @Mapping(source = "shipment.order.orderCode", target = "orderCode")
  @Mapping(source = "shipment.carrierName", target = "carrier")
  @Mapping(source = "shipment.trackingNumber", target = "trackingCode")
  @Mapping(source = "shipment.shippingStatus", target = "status")
  @Mapping(target = "recipientName", expression = "java(resolveRecipientName(shipment))")
  @Mapping(target = "recipientPhone", expression = "java(resolveRecipientPhone(shipment))")
  @Mapping(target = "shippingAddress", expression = "java(resolveShippingAddress(shipment))")
  @Mapping(source = "shipment.order.finalAmount", target = "orderTotalAmount")
  @Mapping(target = "codAmount", expression = "java(resolveCodAmount(shipment))")
  @Mapping(source = "shipment.order.status", target = "orderStatus")
  @Mapping(source = "paymentMethod", target = "paymentMethod")
  @Mapping(target = "updatedBy", expression = "java(formatUpdatedBy(shipment.getUpdatedBy()))")
  AdminShipmentItemResponse toAdminShipmentItem(Shipment shipment, String paymentMethod);

  default String resolveRecipientName(Shipment s) {
    if (s.getRecipientName() != null && !s.getRecipientName().isBlank()) return s.getRecipientName();
    return s.getOrder() != null ? s.getOrder().getRecipientName() : null;
  }

  default String resolveRecipientPhone(Shipment s) {
    if (s.getRecipientPhone() != null && !s.getRecipientPhone().isBlank()) return s.getRecipientPhone();
    return s.getOrder() != null ? s.getOrder().getRecipientPhone() : null;
  }

  default String resolveShippingAddress(Shipment s) {
    if (s.getShippingAddress() != null && !s.getShippingAddress().isBlank()) return s.getShippingAddress();
    return s.getOrder() != null ? s.getOrder().getShippingAddress() : null;
  }

  default java.math.BigDecimal resolveCodAmount(Shipment s) {
    if (s.getCodAmount() != null) return s.getCodAmount();
    if (s.getOrder() != null && "COD".equalsIgnoreCase(s.getOrder().getPaymentMethod())) {
      return s.getOrder().getFinalTotal();
    }
    return java.math.BigDecimal.ZERO;
  }

  default String formatUpdatedBy(User updatedBy) {
    return updatedBy != null ? updatedBy.getFullName() + " (" + updatedBy.getRole() + ")" : null;
  }
}
