package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestockedItemResponse {
    Long variantId;
    String sku;
    Integer quantityRestocked;
    Integer newOnHandQty;
    Integer newAvailableQty;
}
