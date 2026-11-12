package com.wedbackendservice.nextgen.dto.session;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UploadSessionData {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";

    String uploadId;
    Long ownerId;
    Long productId;
    String storageType;
    String bucket;
    String objectKey;
    String originalFileName;
    String contentType;
    Long expectedSize;
    Long actualSize;
    String etag;
    String status;
    Long presignedExpiresAtMillis;
    Long createdAtMillis;
}
