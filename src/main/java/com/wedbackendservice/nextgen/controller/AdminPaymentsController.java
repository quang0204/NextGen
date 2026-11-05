package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminPaymentListResponse;
import com.wedbackendservice.nextgen.dto.response.admin.PaymentLedgerSummaryResponse;
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
     * API 10.7: Danh sách giao dịch thanh toán (STAFF / ADMIN)
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
     * API 10.8: Tổng hợp sổ thu / đối soát thanh toán (STAFF / ADMIN)
     */
    @GetMapping("/summary")
    public ApiResponse<PaymentLedgerSummaryResponse> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ApiResponse.ok(adminPaymentsService.getLedgerSummary(fromDate, toDate));
    }
}
