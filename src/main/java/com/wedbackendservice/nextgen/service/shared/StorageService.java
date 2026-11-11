package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

public interface StorageService {

    StorageProvider getProvider();

    String resolveBucket(StorageType storageType);

    PresignedUploadInfo generatePresignedUploadUrl(
            StorageType storageType,
            String objectKey,
            String contentType,
            Duration expireAfter);

    URL generatePresignedDownloadUrl(StorageType storageType, String objectKey, Duration expireAfter);

    URL generatePresignedDownloadUrl(String bucket, String objectKey, Duration expireAfter);

    String getAccessUrl(String bucket, String objectKey);

    String getObjectUrl(String bucket, String objectKey);

    boolean exists(String bucket, String objectKey);

    Optional<StoredObjectMeta> getObjectMetadata(String bucket, String objectKey);

    byte[] readObjectHeader(String bucket, String objectKey, int maxBytes);

    void deleteObject(String bucket, String objectKey);

    List<StoredObjectSummary> listObjects(String bucket, String prefix);

    record PresignedUploadInfo(
            String uploadUrl,
            String bucketName,
            String objectKey,
            StorageProvider provider,
            long expiresInSeconds
    ) {
    }

    record StoredObjectMeta(
            long contentLength,
            String contentType,
            String eTag
    ) {
    }

    record StoredObjectSummary(
            String objectKey,
            long lastModifiedEpochMs
    ) {
    }
}
