package com.wedbackendservice.nextgen.module.voucher.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePromotionResponse {
  Long promotionId;
  String name;
  Integer appliedVariantsCount;
  String status;
  String startAt;
  String endAt;
  String createdAt;
}
