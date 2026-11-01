package com.wedbackendservice.nextgen.dto.response.admin;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserListResponse {
    List<AdminUserItemResponse> items;
    PaginationResponse pagination;
}
