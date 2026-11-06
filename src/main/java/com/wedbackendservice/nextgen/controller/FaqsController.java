package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.faq.FaqGroupResponse;
import com.wedbackendservice.nextgen.service.FaqsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqsController {

    private final FaqsService faqsService;

    /**
     * API 12.6: Xem câu hỏi thường gặp.
     */
    @GetMapping
    public ApiResponse<List<FaqGroupResponse>> getFaqs(
            @RequestParam(required = false) String category) {
        return ApiResponse.ok(faqsService.getPublicFaqGroups(category));
    }
}
