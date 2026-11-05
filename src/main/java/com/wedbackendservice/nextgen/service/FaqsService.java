package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.dto.response.faq.FaqListResponse;
import com.wedbackendservice.nextgen.dto.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.entity.FAQ;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.FAQRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqsService {

    private final FAQRepository faqRepository;
    private final SecurityHelper securityHelper;

    @Transactional(readOnly = true)
    public FaqListResponse getPublicFaqs(String category, String search, int page, int size) {
        return query(true, category, search, page, size);
    }

    @Transactional(readOnly = true)
    public FaqResponse getPublicFaq(Long faqId) {
        FAQ faq = requireFaq(faqId);
        if (!Boolean.TRUE.equals(faq.getIsActive())) {
            throw new AppException(ErrorCode.ERR_FAQ_NOT_FOUND);
        }
        return toResponse(faq);
    }

    @Transactional(readOnly = true)
    public FaqListResponse getAdminFaqs(Boolean isActive, String category, String search, int page, int size) {
        securityHelper.authenticateStaffOrAdmin();
        return query(isActive, category, search, page, size);
    }

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

    @Transactional
    public FaqResponse updateFaq(Long faqId, FaqRequest request) {
        securityHelper.authenticateAdmin();
        FAQ faq = requireFaq(faqId);
        String question = request.getQuestion().trim();
        if (!question.equalsIgnoreCase(faq.getQuestion())
                && faqRepository.existsByQuestionIgnoreCase(question)) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Câu hỏi này đã tồn tại trong hệ thống.");
        }

        faq.setQuestion(question);
        faq.setAnswer(request.getAnswer().trim());
        faq.setCategory(request.getCategory());
        if (request.getDisplayOrder() != null) {
            faq.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            faq.setIsActive(request.getIsActive());
        }
        return toResponse(faqRepository.save(faq));
    }

    @Transactional
    public void deleteFaq(Long faqId) {
        securityHelper.authenticateAdmin();
        FAQ faq = requireFaq(faqId);
        faqRepository.delete(faq);
    }

    private FaqListResponse query(Boolean isActive, String category, String search, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(safePage - 1, safeSize,
                Sort.by(Sort.Direction.ASC, "displayOrder").and(Sort.by(Sort.Direction.DESC, "createdAt")));

        Page<FAQ> result = faqRepository.findAll(buildSpecification(isActive, category, search), pageable);

        return FaqListResponse.builder()
                .items(result.getContent().stream().map(this::toResponse).toList())
                .total(result.getTotalElements())
                .page(safePage)
                .size(safeSize)
                .totalPages(result.getTotalPages())
                .hasNext(result.hasNext())
                .build();
    }

    private Specification<FAQ> buildSpecification(Boolean isActive, String category, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (isActive != null) {
                predicates.add(cb.equal(root.get("isActive"), isActive));
            }
            if (category != null && !category.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("category")), category.trim().toLowerCase()));
            }
            if (search != null && !search.isBlank()) {
                String like = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("question")), like),
                        cb.like(cb.lower(root.get("answer")), like)));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private FAQ requireFaq(Long faqId) {
        return faqRepository.findById(faqId)
                .orElseThrow(() -> new AppException(ErrorCode.ERR_FAQ_NOT_FOUND));
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
