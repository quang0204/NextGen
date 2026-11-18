package com.wedbackendservice.nextgen.core.dto.session;

import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.LocalDateTime;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
  @CreatedDate LocalDateTime createdAt;
}
