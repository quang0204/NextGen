package com.wedbackendservice.nextgen.dto.client.response.auth;

import com.wedbackendservice.nextgen.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
    @Builder.Default
    String tokenType = "Bearer";
    String accessToken;
    String refreshToken;
    User users;
}
