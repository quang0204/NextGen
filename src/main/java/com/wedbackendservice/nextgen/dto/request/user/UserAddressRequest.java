package com.wedbackendservice.nextgen.dto.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressRequest {
    @NotBlank(message = "Tên người nhận không được để trống")
    String recipientName;

    @NotBlank(message = "Số điện thoại không được để trống")
    String phone;

    @NotBlank(message = "Địa chỉ chi tiết không được để trống")
    String addressLine;

    Boolean isDefault;
}
