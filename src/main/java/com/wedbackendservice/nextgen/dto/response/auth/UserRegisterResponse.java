package com.wedbackendservice.nextgen.dto.response.auth;

import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
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
