package com.wedbackendservice.nextgen.dto.client.request.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResendOtpRequest {
    @NotBlank(message = "sessionToken không được để trống")
    String sessionToken;
}
