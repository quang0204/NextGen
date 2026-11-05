package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.ActivePromotionResponse;
import com.wedbackendservice.nextgen.service.PromotionsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        List<ActivePromotionResponse> data = promotionsService.getActivePromotions();
        String message = data.isEmpty()
                ? "Hiện tại không có chiến dịch Flash Sale nào đang hoạt động"
                : "Lấy danh sách khuyến mãi đang diễn ra thành công";
        return ApiResponse.ok(message, data);
    }
}
