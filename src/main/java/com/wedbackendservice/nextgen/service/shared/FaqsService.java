package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.client.response.faq.FaqGroupResponse;
import com.wedbackendservice.nextgen.dto.client.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.dto.portal.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.entity.FAQ;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.FaqMapper;
import com.wedbackendservice.nextgen.repository.FAQRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FaqsService {

  private final FAQRepository faqRepository;
  private final SecurityHelper securityHelper;
  private final FaqMapper faqMapper;

  /** API 12.6: xem câu hỏi thường gặp, nhóm theo danh mục. */
  @Transactional(readOnly = true)
  public List<FaqGroupResponse> getPublicFaqGroups(String category) {
    List<FAQ> faqs =
        (category != null && !category.isBlank())
            ? faqRepository.findByCategoryIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAsc(
                category.trim())
            : faqRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    Map<String, List<FaqResponse>> grouped = new LinkedHashMap<>();

    for (FAQ faq : faqs) {
      String key =
          faq.getCategory() == null || faq.getCategory().isBlank() ? "Khác" : faq.getCategory();
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(faqMapper.toResponse(faq));
    }

    return grouped.entrySet().stream()
        .map(
            entry ->
                FaqGroupResponse.builder().category(entry.getKey()).items(entry.getValue()).build())
        .toList();
  }

  /** API 12.7: thêm câu hỏi FAQ (STAFF / ADMIN). */
  @Transactional
  public FaqResponse createFaq(FaqRequest request) {
    securityHelper.authenticateAdmin();
    String question = request.getQuestion().trim();
    if (faqRepository.existsByQuestionIgnoreCase(question)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Câu hỏi này đã tồn tại trong hệ thống.");
    }

    FAQ faq =
        FAQ.builder()
            .question(question)
            .answer(request.getAnswer().trim())
            .category(request.getCategory())
            .displayOrder(request.getDisplayOrder() == null ? 0 : request.getDisplayOrder())
            .isActive(request.getIsActive() == null || request.getIsActive())
            .build();
    return faqMapper.toResponse(faqRepository.save(faq));
  }

  @Transactional(readOnly = true)
  public List<FaqResponse> getAdminFaqs() {
    securityHelper.authenticateStaffOrAdmin();
    return faqRepository
        .findAll(
            org.springframework.data.domain.Sort.by(
                org.springframework.data.domain.Sort.Direction.ASC, "displayOrder"))
        .stream()
        .map(faqMapper::toResponse)
        .toList();
  }

  @Transactional
  public FaqResponse updateFaq(Long id, FaqRequest request) {
    securityHelper.authenticateStaffOrAdmin();
    FAQ faq =
        faqRepository
            .findById(id)
            .orElseThrow(
                () -> new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy FAQ ID " + id));
    faq.setQuestion(request.getQuestion().trim());
    faq.setAnswer(request.getAnswer().trim());
    faq.setCategory(request.getCategory());
    if (request.getDisplayOrder() != null) {
      faq.setDisplayOrder(request.getDisplayOrder());
    }
    if (request.getIsActive() != null) {
      faq.setIsActive(request.getIsActive());
    }
    return faqMapper.toResponse(faqRepository.save(faq));
  }

  @Transactional
  public void deleteFaq(Long id) {
    securityHelper.authenticateStaffOrAdmin();
    if (!faqRepository.existsById(id)) {
      throw new AppException(ErrorCode.INVALID_REQUEST, "Không tìm thấy FAQ ID " + id);
    }
    faqRepository.deleteById(id);
  }
}
