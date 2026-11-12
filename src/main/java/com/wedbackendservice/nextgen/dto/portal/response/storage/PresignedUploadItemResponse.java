package com.wedbackendservice.nextgen.dto.portal.response.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresignedUploadItemResponse {
    String uploadId;
    String uploadUrl;
    String bucket;
    String objectKey;
    String originalFileName;
    String contentType;
    Long fileSize;
    Instant expiresAt;
    Long expiresAtEpochMs;
    Integer expiresInSeconds;
    Map<String, String> requiredHeaders;
}
