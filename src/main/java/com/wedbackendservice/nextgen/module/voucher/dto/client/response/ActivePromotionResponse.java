package com.wedbackendservice.nextgen.module.voucher.dto.client.response;

import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ActivePromotionResponse {
  Long promotionId;
  String name;
  String description;
  String startAt;
  String endAt;
  String status;
  List<ActivePromotionVariantResponse> variants;
}
