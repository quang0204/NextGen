package com.wedbackendservice.nextgen.module.user.dto.client.response;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserMeResponse {
  Long userId;
  Role role;
  String fullName;
  String email;
  String phone;
  UserStatus status;
  AddressResponse defaultAddress;
}
