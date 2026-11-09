package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.response.admin.AdminPaymentListResponse;
import com.wedbackendservice.nextgen.dto.portal.response.admin.PaymentLedgerSummaryResponse;
import com.wedbackendservice.nextgen.service.AdminPaymentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentsController {

    private final AdminPaymentsService adminPaymentsService;

    /**
     * API 10.3: Sổ cái thu chi và đối soát thanh toán.
     */
    @GetMapping
    public ApiResponse<AdminPaymentListResponse> getPayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(
                adminPaymentsService.getPayments(page, size, status, method, search, fromDate, toDate));
    }

    /**
     * Tổng hợp đối soát theo khoảng ngày.
     */
    @GetMapping("/summary")
    public ApiResponse<PaymentLedgerSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(adminPaymentsService.getLedgerSummary(fromDate, toDate));
    }
}
