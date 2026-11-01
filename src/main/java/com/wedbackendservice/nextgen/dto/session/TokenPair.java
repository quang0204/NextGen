package com.wedbackendservice.nextgen.dto.session;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class TokenPair {
    String accessToken;
    String refreshToken;
}
