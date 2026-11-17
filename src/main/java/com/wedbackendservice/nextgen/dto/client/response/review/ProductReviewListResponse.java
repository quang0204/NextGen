package com.wedbackendservice.nextgen.dto.client.response.review;

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
public class ProductReviewListResponse {
  List<ReviewResponse> items;
  long totalReviews;
  double averageRating;
  PageResponse<?> pageInfo;
}
