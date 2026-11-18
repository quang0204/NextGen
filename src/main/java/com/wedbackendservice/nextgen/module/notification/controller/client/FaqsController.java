package com.wedbackendservice.nextgen.module.notification.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.notification.dto.client.response.FaqGroupResponse;
import com.wedbackendservice.nextgen.module.notification.service.FaqsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/faqs")
@RequiredArgsConstructor
public class FaqsController {

  private final FaqsService faqsService;

  /** API 12.6: Xem câu hỏi thường gặp. */
  @GetMapping
  public ApiResponse<List<FaqGroupResponse>> getFaqs(
      @RequestParam(required = false) String category) {
    return ApiResponse.ok(faqsService.getPublicFaqGroups(category));
  }
}
