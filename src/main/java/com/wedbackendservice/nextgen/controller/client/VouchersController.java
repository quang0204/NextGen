package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.request.voucher.ApplyVoucherRequest;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.response.voucher.ApplyVoucherResponse;
import com.wedbackendservice.nextgen.dto.client.response.voucher.ValidVoucherResponse;
import com.wedbackendservice.nextgen.service.shared.VouchersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/vouchers")
@RequiredArgsConstructor
public class VouchersController {

    private final VouchersService vouchersService;

    /**
     * API 6.5: [GET] /api/v1/vouchers/valid — Danh sách mã giảm giá còn hiệu lực (Public / Auth)
     */
    @GetMapping("/valid")
    public ApiResponse<List<ValidVoucherResponse>> getValidVouchers(@RequestParam(required = false) BigDecimal minOrderValue) {
        return ApiResponse.ok("Lấy danh sách mã giảm giá khả dụng thành công", vouchersService.getValidVouchers(minOrderValue));
    }

    /**
     * API 6.6: [POST] /api/v1/vouchers/apply — Kiểm tra hợp lệ và tính mức giảm voucher (Authenticated)
     */
    @PostMapping("/apply")
    public ApiResponse<ApplyVoucherResponse> applyVoucher(@Valid @RequestBody ApplyVoucherRequest request) {
        return ApiResponse.ok("Áp dụng mã giảm giá thành công!", vouchersService.applyVoucher(request));
    }
}
