package com.wedbackendservice.nextgen.dto.portal.request.promotion;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdatePromotionStatusRequest {
    @NotBlank(message = "Trạng thái khuyến mãi không được để trống")
    String status;

    String reason;
}
