package com.wedbackendservice.nextgen.module.order.dto.portal.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

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

  @Valid List<@Valid RestockItemRequest> restockItems;

  String rejectReason;
}
