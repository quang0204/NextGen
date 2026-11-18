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
public class UserRegisterResponse {
  Long userId;
  String fullName;
  String phone;
  String email;
  Role role;
  UserStatus status;
}
