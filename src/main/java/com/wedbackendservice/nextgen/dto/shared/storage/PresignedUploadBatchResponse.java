package com.wedbackendservice.nextgen.dto.shared.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PresignedUploadBatchResponse {
    List<PresignedUploadItemResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PresignedUploadItemResponse {
        String uploadId;
        StorageProvider provider;
        StorageType storageType;
        String bucketName;
        String objectKey;
        String uploadUrl;
        String httpMethod;
        String expiration;
        Long expiresInSeconds;
        Map<String, String> requiredHeaders;
    }
}
