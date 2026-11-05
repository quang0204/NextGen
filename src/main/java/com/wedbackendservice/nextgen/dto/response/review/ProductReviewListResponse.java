package com.wedbackendservice.nextgen.dto.response.review;

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
    int page;
    int size;
    int totalPages;
    boolean hasNext;
}
