package com.wedbackendservice.nextgen.dto.response.inventory;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LowStockAlertResponse {
    Integer totalAlerts;
    List<LowStockItemResponse> items;
}
