package com.wedbackendservice.nextgen.module.notification.mapper;

import com.wedbackendservice.nextgen.module.notification.dto.client.response.NotificationResponse;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
  @Mapping(source = "order.orderId", target = "orderId")
  @Mapping(source = "order.orderCode", target = "orderCode")
  NotificationResponse toResponse(Notification notification);
}
