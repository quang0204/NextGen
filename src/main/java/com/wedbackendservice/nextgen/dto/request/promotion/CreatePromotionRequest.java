package com.wedbackendservice.nextgen.dto.request.promotion;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreatePromotionRequest {
    @NotBlank(message = "Tên chiến dịch khuyến mãi không được để trống")
    String name;

    String description;

    @NotBlank(message = "Thời gian bắt đầu (startAt) không được để trống")
    String startAt;

    @NotBlank(message = "Thời gian kết thúc (endAt) không được để trống")
    String endAt;

    @Builder.Default
    String status = "UPCOMING";

    @NotEmpty(message = "Danh sách biến thể áp dụng không được để trống")
    @Valid
    List<PromotionVariantItemRequest> variants;
}
