package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.voucher.ApplyVoucherRequest;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.ApplyVoucherResponse;
import com.wedbackendservice.nextgen.dto.response.voucher.ValidVoucherResponse;
import com.wedbackendservice.nextgen.service.VouchersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
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
    public ApiResponse<List<ValidVoucherResponse>> getValidVouchers(
            @RequestParam(required = false) BigDecimal minOrderValue) {

        List<ValidVoucherResponse> responseData = vouchersService.getValidVouchers(minOrderValue);

        return ApiResponse.<List<ValidVoucherResponse>>builder()
                .success(true)
                .statusCode(200)
                .message("Lấy danh sách mã giảm giá khả dụng thành công")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }

    /**
     * API 6.6: [POST] /api/v1/vouchers/apply — Kiểm tra hợp lệ và tính mức giảm voucher (Authenticated)
     */
    @PostMapping("/apply")
    public ApiResponse<ApplyVoucherResponse> applyVoucher(
            @Valid @RequestBody ApplyVoucherRequest request) {

        ApplyVoucherResponse responseData = vouchersService.applyVoucher(request);

        return ApiResponse.<ApplyVoucherResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Áp dụng mã giảm giá thành công!")
                .data(responseData)
                .timestamp(Instant.now().toString())
                .build();
    }
}
