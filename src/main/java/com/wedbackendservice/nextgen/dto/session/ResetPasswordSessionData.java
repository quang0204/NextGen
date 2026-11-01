package com.wedbackendservice.nextgen.dto.session;

import com.wedbackendservice.nextgen.enums.OtpChannel;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ResetPasswordSessionData {
    Long userId;
    String identity;
    OtpChannel channel;
    String target;
    String otpCode;
    LocalDateTime createdAt;
}
