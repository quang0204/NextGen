package com.wedbackendservice.nextgen.dto.client.response.returnrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

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
