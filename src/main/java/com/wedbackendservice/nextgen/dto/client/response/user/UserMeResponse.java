package com.wedbackendservice.nextgen.dto.client.response.user;

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
public class UserMeResponse {
    Long userId;
    Role role;
    String fullName;
    String email;
    String phone;
    UserStatus status;
    AddressResponse defaultAddress;
}
