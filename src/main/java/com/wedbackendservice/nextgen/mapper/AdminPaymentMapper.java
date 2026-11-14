package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.portal.response.admin.AdminPaymentItemResponse;
import com.wedbackendservice.nextgen.entity.Payment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AdminPaymentMapper {
    @Mapping(source = "order.orderId", target = "orderId")
    @Mapping(source = "order.orderCode", target = "orderCode")
    @Mapping(source = "order.user.fullName", target = "customerName")
    @Mapping(source = "order.user.phone", target = "customerPhone")
    AdminPaymentItemResponse toItem(Payment payment);
}
