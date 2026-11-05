package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.promotion.CreatePromotionRequest;
import com.wedbackendservice.nextgen.dto.request.promotion.UpdatePromotionStatusRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.AdminPromotionItemResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.CreatePromotionResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.UpdatePromotionStatusResponse;
import com.wedbackendservice.nextgen.service.PromotionsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionsController {

    private final PromotionsService promotionsService;

    /**
     * API 6.2: [GET] /api/v1/admin/promotions — Quản lý chiến dịch khuyến mãi (Portal) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminPromotionItemResponse>> getAdminPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ApiResponse.ok("Lấy danh sách chiến dịch khuyến mãi thành công",
                promotionsService.getAdminPromotions(page, size, status));
    }

    /**
     * API 6.3: [POST] /api/v1/admin/promotions — Tạo mới chiến dịch Flash Sale kèm SKU & giá (STAFF / ADMIN)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatePromotionResponse> createPromotion(@Valid @RequestBody CreatePromotionRequest request) {
        return ApiResponse.created("Tạo mới chiến dịch khuyến mãi thành công!",
                promotionsService.createPromotion(request));
    }

    /**
     * API 6.4: [PUT] /api/v1/admin/promotions/{id}/status — Cập nhật trạng thái chiến dịch khuyến mãi (STAFF / ADMIN)
     */
    @PutMapping("/{id:[0-9]+}/status")
    public ApiResponse<UpdatePromotionStatusResponse> updatePromotionStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionStatusRequest request) {
        return ApiResponse.ok("Cập nhật trạng thái chiến dịch khuyến mãi thành công!",
                promotionsService.updatePromotionStatus(id, request));
    }
}
