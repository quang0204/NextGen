package com.wedbackendservice.nextgen.module.product.dto.client.response;

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
public class ProductReviewListResponse {
  List<ReviewResponse> items;
  long totalReviews;
  double averageRating;
  PageResponse<?> pageInfo;
}
