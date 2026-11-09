package com.wedbackendservice.nextgen.controller.client;

import com.wedbackendservice.nextgen.dto.client.request.returnrequest.CreateReturnRequest;
import com.wedbackendservice.nextgen.dto.client.response.returnrequest.CreateReturnResponse;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.service.shared.ReturnsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/returns")
@RequiredArgsConstructor
public class ReturnsController {

    private final ReturnsService returnsService;

    /**
     * API 11.1: [POST] /api/v1/returns — Khách hàng tạo phiếu yêu cầu đổi trả (Authenticated)
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateReturnResponse> createReturn(@Valid @RequestBody CreateReturnRequest request) {
        CreateReturnResponse response = returnsService.createReturn(request);
        return ApiResponse.created(
                "Gửi yêu cầu đổi trả thành công! Nhân viên CSKH sẽ liên hệ hướng dẫn gửi hàng.",
                response
        );
    }
}
