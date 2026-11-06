package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.dto.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.service.FaqsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqsController {

    private final FaqsService faqsService;

    /**
     * API 12.7: Thêm câu hỏi FAQ.
     */
    @PostMapping
    public ApiResponse<FaqResponse> createFaq(@Valid @RequestBody FaqRequest request) {
        return ApiResponse.created("Thêm FAQ thành công", faqsService.createFaq(request));
    }
}
