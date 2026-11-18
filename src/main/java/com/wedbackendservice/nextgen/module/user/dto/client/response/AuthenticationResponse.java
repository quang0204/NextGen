package com.wedbackendservice.nextgen.module.user.dto.client.response;

import com.wedbackendservice.nextgen.module.user.entity.User;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuthenticationResponse {
  @Builder.Default String tokenType = "Bearer";
  String accessToken;
  String refreshToken;
  User users;
}
