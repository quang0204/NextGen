package com.wedbackendservice.nextgen.module.user.dto.portal.request;

import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserStatusRequest {
  @NotNull(message = "Trạng thái không được để trống")
  UserStatus status;

  String lockReason;
}
