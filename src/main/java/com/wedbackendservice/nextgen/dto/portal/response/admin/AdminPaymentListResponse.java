package com.wedbackendservice.nextgen.dto.portal.response.admin;

import com.wedbackendservice.nextgen.dto.common.PageResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPaymentListResponse {
  List<AdminPaymentItemResponse> items;
  PageResponse<?> pageInfo;
}
