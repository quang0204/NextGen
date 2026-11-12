package com.wedbackendservice.nextgen.dto.portal.response.storage;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadItemResponse {
    String uploadId;
    String objectKey;
    String uploadUrl;
    Instant expiresAt;
}
