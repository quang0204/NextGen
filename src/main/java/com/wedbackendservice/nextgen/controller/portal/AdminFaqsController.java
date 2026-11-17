package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.client.response.faq.FaqResponse;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.faq.FaqRequest;
import com.wedbackendservice.nextgen.service.shared.FaqsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/faqs")
@RequiredArgsConstructor
public class AdminFaqsController {

  private final FaqsService faqsService;

  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @GetMapping
  public ApiResponse<List<FaqResponse>> getAdminFaqs() {
    return ApiResponse.ok("Lấy danh sách FAQ thành công", faqsService.getAdminFaqs());
  }

  /** API 12.7: Thêm câu hỏi FAQ. */
  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PostMapping
  public ApiResponse<FaqResponse> createFaq(@Valid @RequestBody FaqRequest request) {
    return ApiResponse.created("Thêm FAQ thành công", faqsService.createFaq(request));
  }

  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @PutMapping("/{id:[0-9]+}")
  public ApiResponse<FaqResponse> updateFaq(
      @PathVariable Long id, @Valid @RequestBody FaqRequest request) {
    return ApiResponse.ok("Cập nhật FAQ thành công", faqsService.updateFaq(id, request));
  }

  @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
  @DeleteMapping("/{id:[0-9]+}")
  public ApiResponse<Void> deleteFaq(@PathVariable Long id) {
    faqsService.deleteFaq(id);
    return ApiResponse.ok("Xóa FAQ thành công", null);
  }
}
