package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.wedbackendservice.nextgen.dto.common.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLogListResponse {
    List<AuditLogResponse> items;
    PageResponse<?> pageInfo;
}
