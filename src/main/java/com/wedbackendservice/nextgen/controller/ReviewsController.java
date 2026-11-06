package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.review.ReviewCreateRequest;
import com.wedbackendservice.nextgen.dto.response.review.ProductReviewListResponse;
import com.wedbackendservice.nextgen.dto.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.service.ReviewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;

    /**
     * API 12.1: Xem đánh giá của sản phẩm.
     */
    @GetMapping("/product/{prodId}")
    public ApiResponse<ProductReviewListResponse> getProductReviews(
            @PathVariable Long prodId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(reviewsService.getProductReviews(prodId, page, size));
    }

    /**
     * API 12.2: Gửi đánh giá.
     */
    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.created("Gửi đánh giá thành công", reviewsService.createReview(request));
    }
}
