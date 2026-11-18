package com.wedbackendservice.nextgen.module.user.dto.client.response;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
  Long userId;
  Role role;
  String fullName;
  String email;
  String phone;
  UserStatus status;
  LocalDateTime createdAt;
  LocalDateTime updatedAt;
}
