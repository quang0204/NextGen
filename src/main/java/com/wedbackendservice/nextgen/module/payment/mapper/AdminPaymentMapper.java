package com.wedbackendservice.nextgen.module.payment.mapper;

import com.wedbackendservice.nextgen.module.payment.dto.portal.response.AdminPaymentItemResponse;
import com.wedbackendservice.nextgen.module.payment.entity.Payment;
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
