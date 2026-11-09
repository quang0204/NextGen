package com.wedbackendservice.nextgen.dto.client.response.review;

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
public class ProductReviewListResponse {
    List<ReviewResponse> items;
    long totalReviews;
    double averageRating;
    PageInfoResponse pageInfo;
}
