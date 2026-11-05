package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.dto.response.faq.FaqListResponse;
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
     * API 12.17: Quản trị danh sách FAQ (gồm cả ẩn/hiện).
     */
    @GetMapping
    public ApiResponse<FaqListResponse> getFaqs(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(faqsService.getAdminFaqs(isActive, category, search, page, size));
    }

    /**
     * API 12.18: Tạo FAQ mới.
     */
    @PostMapping
    public ApiResponse<FaqResponse> createFaq(@Valid @RequestBody FaqRequest request) {
        return ApiResponse.created("Tạo FAQ thành công", faqsService.createFaq(request));
    }

    /**
     * API 12.19: Cập nhật FAQ.
     */
    @PutMapping("/{faqId}")
    public ApiResponse<FaqResponse> updateFaq(@PathVariable Long faqId,
                                              @Valid @RequestBody FaqRequest request) {
        return ApiResponse.ok("Cập nhật FAQ thành công", faqsService.updateFaq(faqId, request));
    }

    /**
     * API 12.20: Xóa FAQ.
     */
    @DeleteMapping("/{faqId}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long faqId) {
        faqsService.deleteFaq(faqId);
        return ApiResponse.<Void>ok("Xóa FAQ thành công", null);
    }
}
