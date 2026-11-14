package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.portal.response.shipment.AdminShipmentItemResponse;
import com.wedbackendservice.nextgen.entity.Shipment;
import com.wedbackendservice.nextgen.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ShipmentMapper {
    @Mapping(source = "shipment.order.orderId", target = "orderId")
    @Mapping(source = "shipment.order.orderCode", target = "orderCode")
    @Mapping(source = "shipment.carrierName", target = "carrier")
    @Mapping(source = "shipment.trackingNumber", target = "trackingCode")
    @Mapping(source = "shipment.shippingStatus", target = "status")
    @Mapping(source = "shipment.order.recipientName", target = "recipientName")
    @Mapping(source = "shipment.order.recipientPhone", target = "recipientPhone")
    @Mapping(source = "shipment.order.shippingAddress", target = "shippingAddress")
    @Mapping(source = "shipment.order.finalAmount", target = "orderTotalAmount")
    @Mapping(source = "paymentMethod", target = "paymentMethod")
    @Mapping(target = "updatedBy", expression = "java(formatUpdatedBy(shipment.getUpdatedBy()))")
    AdminShipmentItemResponse toAdminShipmentItem(Shipment shipment, String paymentMethod);

    default String formatUpdatedBy(User updatedBy) {
        return updatedBy != null ? updatedBy.getFullName() + " (" + updatedBy.getRole() + ")" : null;
    }
}
