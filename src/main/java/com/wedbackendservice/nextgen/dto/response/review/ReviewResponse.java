package com.wedbackendservice.nextgen.dto.response.review;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReviewResponse {
    Long reviewId;
    Long orderItemId;
    Long productId;
    String productName;
    String variantName;
    String customerName;
    Integer rating;
    String comment;
    Boolean isVerifiedPurchase;
    String status;
    String reply;
    LocalDateTime repliedAt;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
