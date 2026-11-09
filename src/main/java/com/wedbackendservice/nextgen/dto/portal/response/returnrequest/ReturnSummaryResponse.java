package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ReturnSummaryResponse {
    Long totalPending;
    Long totalApproved;
    Long totalRejected;
}
