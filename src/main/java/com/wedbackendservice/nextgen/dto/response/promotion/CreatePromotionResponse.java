package com.wedbackendservice.nextgen.dto.response.promotion;

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
