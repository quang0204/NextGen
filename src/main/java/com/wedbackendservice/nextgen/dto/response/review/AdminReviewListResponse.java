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
public class AdminReviewListResponse {
    List<ReviewResponse> items;
    int page;
    int size;
    int totalPages;
    long totalElements;
    boolean hasNext;
}
