package com.wedbackendservice.nextgen.module.voucher.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePromotionStatusResponse {
  Long promotionId;
  String status;
  String updatedAt;
}
