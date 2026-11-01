package com.wedbackendservice.nextgen.dto.response.admin;

import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminUserItemResponse {
    Long userId;
    String fullName;
    String email;
    String phone;
    Role role;
    UserStatus status;
    String lockReason;
    LocalDateTime createdAt;
}
