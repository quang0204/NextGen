package com.wedbackendservice.nextgen.dto.portal.request.product;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProductStatusRequest {
    @NotBlank(message = "status không được để trống")
    String status;

    String reason;
}
