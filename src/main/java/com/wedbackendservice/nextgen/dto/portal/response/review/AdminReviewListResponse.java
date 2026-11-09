package com.wedbackendservice.nextgen.dto.portal.response.review;

import com.wedbackendservice.nextgen.dto.client.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
