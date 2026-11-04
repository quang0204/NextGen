package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.voucher.CreateVoucherRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.AdminVoucherListResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.CreateVoucherResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.ToggleVoucherResponse;
import com.wedbackendservice.nextgen.service.VouchersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVouchersController {

    private final VouchersService vouchersService;

    /**
     * API 6.7: [GET] /api/v1/admin/vouchers — Quản trị danh sách mã giảm giá (Portal) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<AdminVoucherListResponse> getAdminVouchers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status) {

        AdminVoucherListResponse responseData = vouchersService.getAdminVouchers(page, size, keyword, status);

        return ApiResponse.<AdminVoucherListResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách mã giảm giá quản trị thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 6.8: [POST] /api/v1/admin/vouchers — Khởi tạo mã voucher khuyến mãi mới (STAFF / ADMIN)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateVoucherResponse> createVoucher(
            @Valid @RequestBody CreateVoucherRequest request) {

        CreateVoucherResponse responseData = vouchersService.createVoucher(request);

        return ApiResponse.<CreateVoucherResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Khởi tạo mã Voucher thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 6.9: [PATCH] /api/v1/admin/vouchers/{id:[0-9]+}/toggle — Bật / Tắt kích hoạt nhanh voucher (STAFF / ADMIN)
     */
    @PatchMapping("/{id:[0-9]+}/toggle")
    public ApiResponse<ToggleVoucherResponse> toggleVoucher(@PathVariable Long id) {
        ToggleVoucherResponse responseData = vouchersService.toggleVoucher(id);

        return ApiResponse.<ToggleVoucherResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Thay đổi trạng thái kích hoạt Voucher thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
