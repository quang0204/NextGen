package com.wedbackendservice.nextgen.dto.response.review;

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
public class ProductReviewListResponse {
    List<ReviewResponse> items;
    long totalReviews;
    double averageRating;
    PageInfoResponse pageInfo;
}
