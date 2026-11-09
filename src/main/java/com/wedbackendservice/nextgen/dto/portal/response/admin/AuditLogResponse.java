package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditLogResponse {
    Long logId;
    Long userId;
    String userName;
    String action;
    String entityName;
    Long entityId;
    String oldValue;
    String newValue;
    LocalDateTime createdAt;
}
