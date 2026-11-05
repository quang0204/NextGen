package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.review.ProductReviewListResponse;
import com.wedbackendservice.nextgen.service.ReviewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * API công khai: đánh giá hiển thị trên trang sản phẩm.
 */
@RestController
@RequestMapping("/api/v1/products/{productId}/reviews")
@RequiredArgsConstructor
public class FrontendReviewsController {

    private final ReviewsService reviewsService;

    /**
     * API 12.5: Danh sách đánh giá công khai của sản phẩm + điểm trung bình.
     */
    @GetMapping
    public ApiResponse<ProductReviewListResponse> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewsService.getProductReviews(productId, page, size));
    }
}
