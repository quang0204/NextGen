package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.review.ReviewReplyRequest;
import com.wedbackendservice.nextgen.dto.response.review.AdminReviewListResponse;
import com.wedbackendservice.nextgen.dto.response.review.ReviewResponse;
import com.wedbackendservice.nextgen.service.ReviewsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewsController {

    private final ReviewsService reviewsService;

    /**
     * API 12.6: Quản trị danh sách đánh giá (lọc theo trạng thái, số sao, từ khóa).
     */
    @GetMapping
    public ApiResponse<AdminReviewListResponse> getReviews(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) String search) {
        return ApiResponse.ok(reviewsService.getAdminReviews(page, size, status, rating, search));
    }

    /**
     * API 12.7: Phản hồi đánh giá của khách hàng.
     */
    @PutMapping("/{reviewId}/reply")
    public ApiResponse<ReviewResponse> replyReview(@PathVariable Long reviewId,
                                                   @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.ok("Phản hồi đánh giá thành công",
                reviewsService.replyReview(reviewId, request));
    }

    /**
     * API 12.8: Ẩn / hiện đánh giá.
     */
    @PutMapping("/{reviewId}/visibility")
    public ApiResponse<ReviewResponse> updateVisibility(@PathVariable Long reviewId,
                                                        @RequestParam boolean visible) {
        return ApiResponse.ok(visible ? "Đã hiển thị đánh giá" : "Đã ẩn đánh giá",
                reviewsService.updateVisibility(reviewId, visible));
    }
}
