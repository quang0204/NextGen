package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
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
public class AdminReturnItemResponse {
    Long returnId;
    Long orderId;
    String orderCode;
    CustomerInfoResponse customer;
    String reason;
    List<String> evidenceImages;
    String status;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    BigDecimal refundAmount;

    String createdAt;
}
