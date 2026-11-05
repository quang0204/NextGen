package com.wedbackendservice.nextgen.dto.portal.response.order;

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
