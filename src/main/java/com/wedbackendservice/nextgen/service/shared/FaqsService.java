package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.portal.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.dto.client.response.faq.FaqGroupResponse;
import com.wedbackendservice.nextgen.dto.client.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.entity.FAQ;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.FAQRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqsService {

    private final FAQRepository faqRepository;
    private final SecurityHelper securityHelper;

    /**
     * API 12.6: xem câu hỏi thường gặp, nhóm theo danh mục.
     */
    @Transactional(readOnly = true)
    public List<FaqGroupResponse> getPublicFaqGroups(String category) {
        List<FAQ> faqs = (category != null && !category.isBlank())
                ? faqRepository.findByCategoryIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAsc(category.trim())
                : faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        Map<String, List<FaqResponse>> grouped = new LinkedHashMap<>();

        for (FAQ faq : faqs) {
            String key = faq.getCategory() == null || faq.getCategory().isBlank()
                    ? "Khác" : faq.getCategory();
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(toResponse(faq));
        }

        return grouped.entrySet().stream()
                .map(entry -> FaqGroupResponse.builder()
                        .category(entry.getKey())
                        .items(entry.getValue())
                        .build())
                .toList();
    }

    /**
     * API 12.7: thêm câu hỏi FAQ (STAFF / ADMIN).
     */
    @Transactional
    public FaqResponse createFaq(FaqRequest request) {
        securityHelper.authenticateAdmin();
        String question = request.getQuestion().trim();
        if (faqRepository.existsByQuestionIgnoreCase(question)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Câu hỏi này đã tồn tại trong hệ thống.");
        }

        FAQ faq = FAQ.builder()
                .question(question)
                .answer(request.getAnswer().trim())
                .category(request.getCategory())
                .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder())
                .isActive(request.getIsActive() == null || request.getIsActive())
                .build();
        return toResponse(faqRepository.save(faq));
    }

    private FaqResponse toResponse(FAQ faq) {
        return FaqResponse.builder()
                .faqId(faq.getFaqId())
                .question(faq.getQuestion())
                .answer(faq.getAnswer())
                .category(faq.getCategory())
                .displayOrder(faq.getDisplayOrder())
                .isActive(faq.getIsActive())
                .createdAt(faq.getCreatedAt())
                .updatedAt(faq.getUpdatedAt())
                .build();
    }
}
