package com.wedbackendservice.nextgen.module.user.dto.portal.request;

import com.wedbackendservice.nextgen.module.user.enums.Role;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateUserRoleRequest {
  @NotNull(message = "Vai trò không được để trống")
  Role role;

  String reason;
}
