package com.wedbackendservice.nextgen.dto.portal.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateProductStatusResponse {
    Long productId;
    String name;
    String previousStatus;
    String newStatus;
    String reason;
    String updatedBy;
    String updatedAt;
}
