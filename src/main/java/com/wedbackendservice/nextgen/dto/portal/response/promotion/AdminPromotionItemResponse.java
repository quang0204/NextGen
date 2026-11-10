package com.wedbackendservice.nextgen.dto.portal.response.promotion;

import com.wedbackendservice.nextgen.dto.client.response.promotion.ActivePromotionVariantResponse;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
