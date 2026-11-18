package com.wedbackendservice.nextgen.module.user.dto.portal.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateInternalUserResponse {
  Long userId;
  String fullName;
  String email;
  String phone;
  String role;
  String status;
  LocalDateTime createdAt;
}
