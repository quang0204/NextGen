package com.wedbackendservice.nextgen.dto.portal.response.order;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ExportedItemResponse {
    String sku;
    Integer quantity;
    Integer remainingOnHand;
}
