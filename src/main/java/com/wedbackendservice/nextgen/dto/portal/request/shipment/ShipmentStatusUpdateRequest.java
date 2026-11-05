package com.wedbackendservice.nextgen.dto.portal.request.shipment;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShipmentStatusUpdateRequest {
    @NotBlank(message = "Trạng thái không được để trống")
    String status;

    String note;
}
