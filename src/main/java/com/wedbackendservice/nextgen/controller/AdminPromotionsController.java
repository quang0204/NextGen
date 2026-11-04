package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.promotion.CreatePromotionRequest;
import com.wedbackendservice.nextgen.dto.request.promotion.UpdatePromotionStatusRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.AdminPromotionListResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.CreatePromotionResponse;
import com.wedbackendservice.nextgen.dto.response.promotion.UpdatePromotionStatusResponse;
import com.wedbackendservice.nextgen.service.PromotionsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
public class AdminPromotionsController {

    private final PromotionsService promotionsService;

    /**
     * API 6.2: [GET] /api/v1/admin/promotions — Quản lý chiến dịch khuyến mãi (Portal) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<AdminPromotionListResponse> getAdminPromotions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "ALL") String status) {

        AdminPromotionListResponse responseData = promotionsService.getAdminPromotions(page, size, status);

        return ApiResponse.<AdminPromotionListResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách chiến dịch khuyến mãi thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 6.3: [POST] /api/v1/admin/promotions — Tạo mới chiến dịch Flash Sale kèm SKU & giá (STAFF / ADMIN)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreatePromotionResponse> createPromotion(
            @Valid @RequestBody CreatePromotionRequest request) {

        CreatePromotionResponse responseData = promotionsService.createPromotion(request);

        return ApiResponse.<CreatePromotionResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Tạo mới chiến dịch khuyến mãi thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 6.4: [PUT] /api/v1/admin/promotions/{id}/status — Cập nhật trạng thái chiến dịch khuyến mãi (STAFF / ADMIN)
     */
    @PutMapping("/{id:[0-9]+}/status")
    public ApiResponse<UpdatePromotionStatusResponse> updatePromotionStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromotionStatusRequest request) {

        UpdatePromotionStatusResponse responseData = promotionsService.updatePromotionStatus(id, request);

        return ApiResponse.<UpdatePromotionStatusResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Cập nhật trạng thái chiến dịch khuyến mãi thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
