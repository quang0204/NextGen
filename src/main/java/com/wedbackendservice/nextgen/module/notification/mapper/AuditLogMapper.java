package com.wedbackendservice.nextgen.module.notification.mapper;

import com.wedbackendservice.nextgen.module.notification.dto.portal.response.AuditLogResponse;
import com.wedbackendservice.nextgen.module.notification.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
  @Mapping(source = "user.userId", target = "userId")
  @Mapping(source = "user.fullName", target = "userName")
  AuditLogResponse toResponse(AuditLog log);
}
