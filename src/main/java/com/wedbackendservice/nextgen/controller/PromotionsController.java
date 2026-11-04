package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.ActivePromotionResponse;
import com.wedbackendservice.nextgen.service.PromotionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionsController {

    private final PromotionsService promotionsService;

    /**
     * API 6.1: [GET] /api/v1/promotions/active — Danh sách Flash Sale đang diễn ra (Public)
     */
    @GetMapping("/active")
    public ApiResponse<List<ActivePromotionResponse>> getActivePromotions() {
        List<ActivePromotionResponse> responseData = promotionsService.getActivePromotions();
        String message = responseData.isEmpty()
                ? "Hiện tại không có chiến dịch Flash Sale nào đang hoạt động"
                : "Lấy danh sách khuyến mãi đang diễn ra thành công";

        return ApiResponse.<List<ActivePromotionResponse>>builder()
                .success(true)
                .statusCode(200)
                .message(message)
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
