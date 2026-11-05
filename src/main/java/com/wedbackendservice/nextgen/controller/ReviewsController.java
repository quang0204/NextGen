package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.review.ReviewCreateRequest;
import com.wedbackendservice.nextgen.dto.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.service.ReviewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API đánh giá dành cho khách hàng đã mua hàng.
 */
@RestController
@RequestMapping("/api/v1/reviews")
@RequiredArgsConstructor
public class ReviewsController {

    private final ReviewsService reviewsService;

    /**
     * API 12.1: Tạo đánh giá cho một sản phẩm trong đơn hàng đã hoàn thành.
     */
    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.created("Gửi đánh giá thành công", reviewsService.createReview(request));
    }

    /**
     * API 12.2: Cập nhật đánh giá của chính mình.
     */
    @PutMapping("/{reviewId}")
    public ApiResponse<ReviewResponse> updateReview(@PathVariable Long reviewId,
                                                    @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.ok("Cập nhật đánh giá thành công",
                reviewsService.updateMyReview(reviewId, request));
    }

    /**
     * API 12.3: Xóa đánh giá của chính mình.
     */
    @DeleteMapping("/{reviewId}")
    public ApiResponse<Void> deleteReview(@PathVariable Long reviewId) {
        reviewsService.deleteMyReview(reviewId);
        return ApiResponse.<Void>ok("Xóa đánh giá thành công", null);
    }

    /**
     * API 12.4: Danh sách đánh giá của tôi.
     */
    @GetMapping("/me")
    public ApiResponse<List<ReviewResponse>> getMyReviews() {
        return ApiResponse.ok(reviewsService.getMyReviews());
    }
}
