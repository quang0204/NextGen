package com.wedbackendservice.nextgen.module.order.dto.client.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateReturnResponse {
  Long returnId;
  Long orderId;
  String orderCode;
  Long requestedBy;
  String status;
  String reason;
  List<String> evidenceImages;
  String createdAt;
}
