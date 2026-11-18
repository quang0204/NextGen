package com.wedbackendservice.nextgen.module.product.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.request.ReviewCreateRequest;
import com.wedbackendservice.nextgen.module.product.dto.client.response.ProductReviewListResponse;
import com.wedbackendservice.nextgen.module.product.dto.client.response.ReviewResponse;
import com.wedbackendservice.nextgen.module.product.service.client.ReviewsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/api/v1/reviews", "/reviews"})
@RequiredArgsConstructor
public class ReviewsController {

  private final ReviewsService reviewsService;

  /** API 12.1: Xem đánh giá của sản phẩm. */
  @GetMapping("/product/{prodId}")
  public ApiResponse<ProductReviewListResponse> getProductReviews(
      @PathVariable Long prodId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "10") int size) {
    return ApiResponse.ok(reviewsService.getProductReviews(prodId, page, size));
  }

  /** API 12.2: Gửi đánh giá. */
  @PostMapping
  public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
      @Valid @RequestBody ReviewCreateRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created("Gửi đánh giá thành công", reviewsService.createReview(request)));
  }

  /** API: Lấy danh sách ID các dòng đơn (orderItemId) mà user hiện tại đã đánh giá. */
  @GetMapping("/my-reviewed-items")
  public ApiResponse<List<Long>> getMyReviewedItemIds() {
    return ApiResponse.ok(
        "Lấy danh sách dòng đơn đã đánh giá thành công", reviewsService.getMyReviewedItemIds());
  }
}
