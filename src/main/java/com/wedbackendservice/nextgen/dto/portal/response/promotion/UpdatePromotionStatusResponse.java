package com.wedbackendservice.nextgen.dto.portal.response.promotion;

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
