package com.wedbackendservice.nextgen.module.user.dto.client.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAddressItemResponse {
  Long addressId;
  String recipientName;
  String phone;
  String addressLine;
  Boolean isDefault;
}
