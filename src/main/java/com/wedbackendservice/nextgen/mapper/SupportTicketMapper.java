package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.client.response.ticket.SupportTicketResponse;
import com.wedbackendservice.nextgen.entity.SupportTicket;
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
