package com.wedbackendservice.nextgen.dto.response.user;

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
