package com.wedbackendservice.nextgen.dto.response.voucher;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ToggleVoucherResponse {
    Long voucherId;
    String code;
    Boolean isActive;
    String updatedAt;
}
