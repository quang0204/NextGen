package com.wedbackendservice.nextgen.dto.session;

import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadSessionData {
    String uploadId;
    Long ownerId;
    Long productId;
    StorageType storageType;
    StorageProvider storageProvider;
    String bucketName;
    String objectKey;
    String originalFileName;
    String contentType;
    Long expectedFileSize;
    Long actualFileSize;
    String eTag;
    UploadStatus status;
    long expiresAtEpochMs;
    long createdAtEpochMs;
}
