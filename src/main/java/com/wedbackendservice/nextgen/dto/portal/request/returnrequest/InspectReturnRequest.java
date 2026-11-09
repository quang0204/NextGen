package com.wedbackendservice.nextgen.dto.portal.request.returnrequest;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InspectReturnRequest {

    @NotBlank(message = "Trạng thái thẩm định (status) không được để trống!")
    String status;

    @NotBlank(message = "Kết quả giám định (inspectionResult) không được để trống!")
    String inspectionResult;

    BigDecimal refundAmount;

    List<RestockItemRequest> restockItems;

    String rejectReason;
}
