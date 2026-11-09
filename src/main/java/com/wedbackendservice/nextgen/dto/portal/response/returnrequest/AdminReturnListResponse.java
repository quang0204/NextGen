package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReturnListResponse {
    List<AdminReturnItemResponse> items;
    ReturnSummaryResponse summary;
    ReturnPaginationResponse pagination;
}
