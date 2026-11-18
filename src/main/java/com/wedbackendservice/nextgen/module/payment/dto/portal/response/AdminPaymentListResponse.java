package com.wedbackendservice.nextgen.module.payment.dto.portal.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
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
