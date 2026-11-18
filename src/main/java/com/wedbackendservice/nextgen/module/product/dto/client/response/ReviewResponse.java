package com.wedbackendservice.nextgen.module.product.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  /** Link xem ảnh đánh giá (lấy từ bảng review_images). */
  List<String> imageUrls;

  Boolean isVerifiedPurchase;
  String status;
  String reply;
  LocalDateTime repliedAt;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
