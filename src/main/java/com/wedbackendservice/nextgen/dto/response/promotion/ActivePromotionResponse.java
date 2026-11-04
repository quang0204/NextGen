package com.wedbackendservice.nextgen.dto.response.promotion;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
