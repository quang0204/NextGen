package com.wedbackendservice.nextgen.module.notification.mapper;

import com.wedbackendservice.nextgen.module.notification.dto.client.response.SupportTicketResponse;
import com.wedbackendservice.nextgen.module.notification.entity.SupportTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SupportTicketMapper {
  @Mapping(source = "user.userId", target = "userId")
  @Mapping(source = "user.fullName", target = "customerName")
  @Mapping(source = "user.email", target = "customerEmail")
  @Mapping(source = "user.phone", target = "customerPhone")
  @Mapping(source = "handledBy.userId", target = "handledById")
  @Mapping(source = "handledBy.fullName", target = "handledByName")
  SupportTicketResponse toResponse(SupportTicket ticket);
}
