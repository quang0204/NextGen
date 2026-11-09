package com.wedbackendservice.nextgen.dto.portal.response.returnrequest;

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
public class InventoryRestockResponse {
    String formCode;
    String txnType;
    List<RestockedItemResponse> restockedItems;
}
