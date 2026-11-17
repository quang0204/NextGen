package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.notification.NotificationResponse;
import com.wedbackendservice.nextgen.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
  @Mapping(source = "order.orderId", target = "orderId")
  @Mapping(source = "order.orderCode", target = "orderCode")
  NotificationResponse toResponse(Notification notification);
}
