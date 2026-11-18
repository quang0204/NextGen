package com.wedbackendservice.nextgen.module.voucher.dto.portal.response;

import com.wedbackendservice.nextgen.module.voucher.dto.client.response.ActivePromotionVariantResponse;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminPromotionItemResponse {
  Long promotionId;
  String name;
  String description;
  String startAt;
  String endAt;
  String status;
  Integer appliedVariantsCount;
  String createdBy;
  String createdAt;
  List<ActivePromotionVariantResponse> variants;
}
