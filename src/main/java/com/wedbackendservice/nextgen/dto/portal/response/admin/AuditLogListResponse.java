package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.wedbackendservice.nextgen.dto.common.PageInfoResponse;
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
    PageInfoResponse pageInfo;
}
