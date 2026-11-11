package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService implements StorageService {

    private final MinioClient internalClient;
    private final MinioClient signingClient;
    private final MinioProperties properties;
    private final Set<String> readyBuckets = ConcurrentHashMap.newKeySet();

    public MinioStorageService(
            @Qualifier("minioClient") MinioClient internalClient,
            @Qualifier("minioPublicClient") MinioClient signingClient,
            MinioProperties properties) {
        this.internalClient = internalClient;
        this.signingClient = signingClient;
        this.properties = properties;
    }

    @Override
    public StorageProvider getProvider() {
        return StorageProvider.MINIO;
    }

    @Override
    public String resolveBucket(StorageType storageType) {
        return properties.resolveBucket(storageType);
    }

    @Override
    public PresignedUploadInfo generatePresignedUploadUrl(
            StorageType storageType,
            String objectKey,
            String contentType,
            Duration expireAfter) {
        String bucket = resolveBucket(storageType);
        ensureBucketExists(bucket);
        try {
            String url = signingClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) expireAfter.toSeconds(), TimeUnit.SECONDS)
                    .extraHeaders(Map.of("Content-Type", contentType))
                    .build());
            return new PresignedUploadInfo(url, bucket, objectKey, StorageProvider.MINIO, expireAfter.toSeconds());
        } catch (Exception e) {
            log.error("MinIO presigned PUT URL failed for bucket={}, key={}: {}", bucket, objectKey, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public URL generatePresignedDownloadUrl(StorageType storageType, String objectKey, Duration expireAfter) {
        return generatePresignedDownloadUrl(resolveBucket(storageType), objectKey, expireAfter);
    }

    @Override
    public URL generatePresignedDownloadUrl(String bucket, String objectKey, Duration expireAfter) {
        try {
            String url = signingClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry((int) expireAfter.toSeconds(), TimeUnit.SECONDS)
                    .build());
            return URI.create(url).toURL();
        } catch (Exception e) {
            log.error("MinIO presigned GET URL failed for bucket={}, key={}: {}", bucket, objectKey, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public String getAccessUrl(String bucket, String objectKey) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return joinUrl(joinUrl(properties.getPublicBaseUrl(), bucket), objectKey);
        }
        if (properties.isPresignGetEnabled()) {
            return generatePresignedDownloadUrl(
                    bucket, objectKey, Duration.ofSeconds(properties.getPresignGetExpirationSeconds())).toString();
        }
        return getObjectUrl(bucket, objectKey);
    }

    @Override
    public String getObjectUrl(String bucket, String objectKey) {
        if (properties.getPublicBaseUrl() != null && !properties.getPublicBaseUrl().isBlank()) {
            return joinUrl(joinUrl(properties.getPublicBaseUrl(), bucket), objectKey);
        }
        return joinUrl(joinUrl(properties.resolvePublicEndpoint(), bucket), objectKey);
    }

    @Override
    public boolean exists(String bucket, String objectKey) {
        return getObjectMetadata(bucket, objectKey).isPresent();
    }

    @Override
    public Optional<StoredObjectMeta> getObjectMetadata(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = internalClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            String eTag = stat.etag() != null ? stat.etag().replace("\"", "") : null;
            return Optional.of(new StoredObjectMeta(stat.size(), stat.contentType(), eTag));
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            int status = e.response() != null ? e.response().code() : 0;
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchBucket".equalsIgnoreCase(code) || status == 404) {
                return Optional.empty();
            }
            log.error("MinIO statObject error for bucket={}, key={}, code={}: {}", bucket, objectKey, code, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        } catch (Exception e) {
            log.error("MinIO statObject failed for bucket={}, key={}: {}", bucket, objectKey, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public byte[] readObjectHeader(String bucket, String objectKey, int maxBytes) {
        try (InputStream is = internalClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(objectKey)
                .offset(0L)
                .length((long) maxBytes)
                .build())) {
            return is.readNBytes(maxBytes);
        } catch (Exception e) {
            log.error("MinIO readObjectHeader failed for bucket={}, key={}: {}", bucket, objectKey, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public void deleteObject(String bucket, String objectKey) {
        try {
            internalClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            log.info("Deleted MinIO object: bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            log.error("MinIO deleteObject failed for bucket={}, key={}: {}", bucket, objectKey, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    @Override
    public List<StoredObjectSummary> listObjects(String bucket, String prefix) {
        List<StoredObjectSummary> items = new ArrayList<>();
        try {
            Iterable<Result<Item>> results = internalClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucket)
                    .prefix(prefix != null ? prefix : "")
                    .recursive(true)
                    .build());
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    long lastModifiedMs = item.lastModified() != null
                            ? item.lastModified().toInstant().toEpochMilli()
                            : 0L;
                    items.add(new StoredObjectSummary(item.objectName(), lastModifiedMs));
                }
            }
            return items;
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            if ("NoSuchBucket".equalsIgnoreCase(code)) {
                return List.of();
            }
            log.error("MinIO listObjects error for bucket={}: {}", bucket, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        } catch (Exception e) {
            log.error("MinIO listObjects failed for bucket={}: {}", bucket, e.getMessage());
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
        }
    }

    private void ensureBucketExists(String bucket) {
        if (readyBuckets.contains(bucket)) {
            return;
        }
        synchronized (readyBuckets) {
            if (readyBuckets.contains(bucket)) {
                return;
            }
            try {
                boolean exists = internalClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
                if (!exists) {
                    internalClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("Created MinIO bucket: {}", bucket);
                }
                readyBuckets.add(bucket);
            } catch (Exception e) {
                log.error("MinIO bucket check/create failed for bucket={}: {}", bucket, e.getMessage());
                throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED);
            }
        }
    }

    private static String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : (baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl);
        String p = path == null ? "" : (path.startsWith("/") ? path.substring(1) : path);
        return base + "/" + p;
    }
}
