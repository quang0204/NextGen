package com.wedbackendservice.nextgen.dto.response.auth;

import com.wedbackendservice.nextgen.enums.OtpChannel;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForgotPasswordResponse {
    String resetToken;
    @Enumerated(EnumType.STRING)
    OtpChannel channel;
    String targetMasked;
    Integer expiresInSeconds;
    Integer cooldownSeconds;
}
