package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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
