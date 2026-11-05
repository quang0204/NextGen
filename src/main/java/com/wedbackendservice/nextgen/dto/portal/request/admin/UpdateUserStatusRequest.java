package com.wedbackendservice.nextgen.dto.portal.request.admin;

import com.wedbackendservice.nextgen.enums.UserStatus;
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
