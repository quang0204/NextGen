package com.wedbackendservice.nextgen.module.user.dto.client.response;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UsersResponse {
  String userId;
  String fullName;
  String email;
  String phone;
  Role role;
  UserStatus status;
}
