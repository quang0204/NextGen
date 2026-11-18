package com.wedbackendservice.nextgen.module.product.dto.portal.response;

import com.wedbackendservice.nextgen.core.dto.PageResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.response.ReviewResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminReviewListResponse {
  List<ReviewResponse> items;
  PageResponse<?> pageInfo;
}
