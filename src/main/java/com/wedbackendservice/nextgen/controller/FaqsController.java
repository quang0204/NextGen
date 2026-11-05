package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.faq.FaqListResponse;
import com.wedbackendservice.nextgen.dto.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.service.FaqsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqsController {

    private final FaqsService faqsService;

    /**
     * API 12.15: Danh sách câu hỏi thường gặp đang hiển thị.
     */
    @GetMapping
    public ApiResponse<FaqListResponse> getFaqs(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(faqsService.getPublicFaqs(category, search, page, size));
    }

    /**
     * API 12.16: Chi tiết một câu hỏi thường gặp.
     */
    @GetMapping("/{faqId}")
    public ApiResponse<FaqResponse> getFaq(@PathVariable Long faqId) {
        return ApiResponse.ok(faqsService.getPublicFaq(faqId));
    }
}
