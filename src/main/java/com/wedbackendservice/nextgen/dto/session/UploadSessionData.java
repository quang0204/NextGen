package com.wedbackendservice.nextgen.dto.session;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UploadSessionData {
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_ATTACHED = "ATTACHED";

    String uploadId;

    @JsonAlias({"ownerId", "userId"})
    Long ownerId;

    Long productId;
    Long brandId;
    Long categoryId;

    String storageType;
    String bucket;
    String objectKey;

    @JsonAlias({"originalFileName", "fileName"})
    String originalFileName;

    String contentType;

    @JsonAlias({"expectedSize", "fileSize"})
    Long expectedSize;

    Long actualSize;
    String etag;
    String status;

    String canonicalUrl;
    String accessUrl;

    @JsonAlias({"presignedExpiresAtMillis", "expiresAt"})
    Long presignedExpiresAtMillis;

    @JsonAlias({"createdAtMillis", "createdAt"})
    Long createdAtMillis;

    public Long getUserId() {
        return ownerId;
    }

    public void setUserId(Long userId) {
        this.ownerId = userId;
    }

    public String getFileName() {
        return originalFileName;
    }

    public void setFileName(String fileName) {
        this.originalFileName = fileName;
    }

    public Long getFileSize() {
        return expectedSize;
    }

    public void setFileSize(Long fileSize) {
        this.expectedSize = fileSize;
    }

    public Long getExpiresAt() {
        return presignedExpiresAtMillis;
    }

    public void setExpiresAt(Long expiresAt) {
        this.presignedExpiresAtMillis = expiresAt;
    }
}
