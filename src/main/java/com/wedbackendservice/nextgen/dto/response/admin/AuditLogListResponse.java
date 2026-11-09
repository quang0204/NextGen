package com.wedbackendservice.nextgen.dto.response.admin;

import com.wedbackendservice.nextgen.dto.response.PageInfoResponse;
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
