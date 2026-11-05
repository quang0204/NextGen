package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.portal.request.voucher.CreateVoucherRequest;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.common.PageResponse;
import com.wedbackendservice.nextgen.dto.portal.response.voucher.AdminVoucherItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.voucher.CreateVoucherResponse;
import com.wedbackendservice.nextgen.dto.portal.response.voucher.ToggleVoucherResponse;
import com.wedbackendservice.nextgen.service.shared.VouchersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/vouchers")
@RequiredArgsConstructor
public class AdminVouchersController {

    private final VouchersService vouchersService;

    /**
     * API 6.7: [GET] /api/v1/admin/vouchers — Quản trị danh sách mã giảm giá (Portal) (STAFF / ADMIN)
     */
    @GetMapping
    public ApiResponse<PageResponse<AdminVoucherItemResponse>> getAdminVouchers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "ALL") String status) {
        return ApiResponse.ok("Lấy danh sách mã giảm giá quản trị thành công",
                vouchersService.getAdminVouchers(page, size, keyword, status));
    }

    /**
     * API 6.8: [POST] /api/v1/admin/vouchers — Khởi tạo mã voucher khuyến mãi mới (STAFF / ADMIN)
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateVoucherResponse> createVoucher(@Valid @RequestBody CreateVoucherRequest request) {
        return ApiResponse.created("Khởi tạo mã Voucher thành công!", vouchersService.createVoucher(request));
    }

    /**
     * API 6.9: [PATCH] /api/v1/admin/vouchers/{id:[0-9]+}/toggle — Bật / Tắt kích hoạt nhanh voucher (STAFF / ADMIN)
     */
    @PatchMapping("/{id:[0-9]+}/toggle")
    public ApiResponse<ToggleVoucherResponse> toggleVoucher(@PathVariable Long id) {
        return ApiResponse.ok("Thay đổi trạng thái kích hoạt Voucher thành công!", vouchersService.toggleVoucher(id));
    }
}
