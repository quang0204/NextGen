package com.wedbackendservice.nextgen.module.order.dto.portal.response;

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
public class InventoryRestockResponse {
  String formCode;
  String txnType;
  List<RestockedItemResponse> restockedItems;
}
