package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.portal.response.admin.AuditLogResponse;
import com.wedbackendservice.nextgen.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.fullName", target = "userName")
    AuditLogResponse toResponse(AuditLog log);
}
