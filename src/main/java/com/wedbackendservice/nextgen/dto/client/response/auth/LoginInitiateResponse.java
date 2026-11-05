package com.wedbackendservice.nextgen.dto.client.response.auth;

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
public class LoginInitiateResponse {
    @Enumerated(EnumType.STRING)
    OtpChannel channel;
    String targetMasked;
    String sessionToken;
    Integer expiresInSeconds;
    Integer cooldownSeconds;
}
