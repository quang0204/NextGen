package com.wedbackendservice.nextgen.dto.client.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderStatusHistoryResponse {
    Long historyId;
    String status;
    String changedBy;
    String note;
    String changedAt;
}
