package com.wedbackendservice.nextgen.dto.shared.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageFileResponse {
    String uploadId;
    Long imageId;
    Long productId;
    StorageProvider provider;
    StorageType storageType;
    String bucketName;
    String objectKey;
    String imageUrl;
    String originalFileName;
    String contentType;
    Long fileSize;
    String eTag;
    UploadStatus status;
}
