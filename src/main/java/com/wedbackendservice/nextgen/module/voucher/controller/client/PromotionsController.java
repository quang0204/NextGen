package com.wedbackendservice.nextgen.module.voucher.controller.client;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.module.voucher.dto.client.response.ActivePromotionResponse;
import com.wedbackendservice.nextgen.module.voucher.service.PromotionsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/promotions")
@RequiredArgsConstructor
public class PromotionsController {

  private final PromotionsService promotionsService;

  /** API 6.1: [GET] /api/v1/promotions/active — Danh sách Flash Sale đang diễn ra (Public) */
  @GetMapping("/active")
  public ApiResponse<List<ActivePromotionResponse>> getActivePromotions() {
    List<ActivePromotionResponse> data = promotionsService.getActivePromotions();
    String message =
        data.isEmpty()
            ? "Hiện tại không có chiến dịch Flash Sale nào đang hoạt động"
            : "Lấy danh sách khuyến mãi đang diễn ra thành công";
    return ApiResponse.ok(message, data);
  }
}
