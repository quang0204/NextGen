package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.order.OrderItemResponse;
import com.wedbackendservice.nextgen.dto.client.response.order.OrderStatusHistoryResponse;
import com.wedbackendservice.nextgen.entity.OrderItem;
import com.wedbackendservice.nextgen.entity.OrderStatusHistory;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.util.DateTimeUtil;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(source = "variant.variantId", target = "variantId")
    @Mapping(source = "variant.product.productId", target = "productId")
    OrderItemResponse toOrderItemResponse(OrderItem oi);

    @Mapping(source = "toStatus", target = "status")
    @Mapping(source = "reason", target = "note")
    @Mapping(target = "changedBy", expression = "java(formatChangedBy(h.getChangedBy()))")
    @Mapping(target = "changedAt", expression = "java(formatCreatedAt(h))")
    OrderStatusHistoryResponse toOrderStatusHistoryResponse(OrderStatusHistory h);

    default String formatChangedBy(User user) {
        if (user == null) {
            return "Hệ thống";
        }
        if (user.getFullName() == null) {
            return "Người dùng";
        }
        String roleName = user.getRole() != null ? user.getRole().name() : "Người dùng";
        return user.getFullName() + " (" + roleName + ")";
    }

    default String formatCreatedAt(OrderStatusHistory h) {
        return DateTimeUtil.formatIsoInstant(h.getCreatedAt());
    }
}
