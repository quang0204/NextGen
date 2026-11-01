package com.wedbackendservice.nextgen.dto.request.admin;

import com.wedbackendservice.nextgen.enums.Role;
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
}
