package com.wedbackendservice.nextgen.module.order.dto.portal.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PipelineSummaryResponse {
  Long pendingPayment;
  Long waitingConfirmation;
  Long confirmed;
  Long preparing;
  Long shipping;
  Long completed;
  Long cancelled;
}
