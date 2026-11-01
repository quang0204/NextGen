package com.wedbackendservice.nextgen.dto.session;

import com.wedbackendservice.nextgen.enums.OtpChannel;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@EntityListeners(AuditingEntityListener.class)
public class OtpSessionData {
    Long userId;
    @Enumerated(EnumType.STRING)
    OtpChannel channel;
    String target;
    String otpCode;
    @CreatedDate
    LocalDateTime createdAt;
}
