package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient minioInternalClient;
    private final MinioClient minioPublicClient;
    private final MinioProperties minioProperties;
    private final Set<String> initializedBuckets = ConcurrentHashMap.newKeySet();

    public MinioStorageService(
            @Qualifier("minioInternalClient") MinioClient minioInternalClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient,
            MinioProperties minioProperties) {
        this.minioInternalClient = minioInternalClient;
        this.minioPublicClient = minioPublicClient;
        this.minioProperties = minioProperties;
    }

    public record ObjectMetadata(long size, String etag, String contentType) {}

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultBuckets() {
        try {
            ensureBucketReady(minioProperties.getBucket().getProducts());
        } catch (Exception e) {
            log.warn("MinIO chưa sẵn sàng khi khởi động ({}). Sẽ tự khởi tạo bucket ở lần upload đầu tiên.", e.getMessage());
        }
    }

    public void ensureBucketReady(String bucket) {
        if (initializedBuckets.contains(bucket)) {
            return;
        }
        try {
            boolean exists = minioInternalClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                minioInternalClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket '{}'", bucket);
            }
            if (minioProperties.isPublicRead()) {
                String policy = """
                        {
                          "Version": "2012-10-17",
                          "Statement": [
                            {
                              "Effect": "Allow",
                              "Principal": {"AWS": ["*"]},
                              "Action": ["s3:GetObject"],
                              "Resource": ["arn:aws:s3:::%s/*"]
                            }
                          ]
                        }
                        """.formatted(bucket);
                minioInternalClient.setBucketPolicy(
                        SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
            }
            initializedBuckets.add(bucket);
        } catch (Exception e) {
            log.error("Failed to initialize MinIO bucket '{}': {}", bucket, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED,
                    "Không thể khởi tạo bucket lưu trữ MinIO: " + e.getMessage());
        }
    }

    public String generatePresignedUploadUrl(String bucket, String objectKey, String contentType, int expirySeconds) {
        ensureBucketReady(bucket);
        try {
            return minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .extraHeaders(Map.of("Content-Type", contentType))
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED,
                    "Không thể tạo URL tải ảnh lên MinIO.");
        }
    }

    public Optional<ObjectMetadata> statObject(String bucket, String objectKey) {
        try {
            StatObjectResponse stat = minioInternalClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            String etag = stat.etag() != null ? stat.etag().replace("\"", "") : null;
            return Optional.of(new ObjectMetadata(stat.size(), etag, stat.contentType()));
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            int status = e.response() != null ? e.response().code() : 0;
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchBucket".equalsIgnoreCase(code) || status == 404) {
                return Optional.empty();
            }
            log.error("MinIO statObject error for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi kiểm tra tệp trên MinIO.");
        } catch (Exception e) {
            log.error("MinIO statObject unexpected error for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Lỗi kiểm tra tệp trên MinIO.");
        }
    }

    public byte[] readObjectHeader(String bucket, String objectKey, int length) {
        try (InputStream is = minioInternalClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .offset(0L)
                        .length((long) length)
                        .build())) {
            return is.readNBytes(length);
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() != null ? e.errorResponse().code() : "";
            int status = e.response() != null ? e.response().code() : 0;
            if ("NoSuchKey".equalsIgnoreCase(code) || "NoSuchBucket".equalsIgnoreCase(code) || status == 404) {
                throw new AppException(ErrorCode.STORAGE_OBJECT_NOT_UPLOADED);
            }
            log.error("MinIO readObjectHeader error for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể đọc dữ liệu tệp trên MinIO.");
        } catch (Exception e) {
            log.error("MinIO readObjectHeader unexpected error for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể đọc dữ liệu tệp trên MinIO.");
        }
    }

    public void deleteObject(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank() || objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            minioInternalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}/{}: {}", bucket, objectKey, e.getMessage());
        }
    }

    public String getFileUrl(String bucket, String objectKey) {
        if (minioProperties.isPublicRead()) {
            String base = (minioProperties.getPublicEndpoint() != null
                    && !minioProperties.getPublicEndpoint().isBlank())
                    ? minioProperties.getPublicEndpoint()
                    : minioProperties.getEndpoint();
            base = base.replaceAll("/+$", "");
            return base + "/" + bucket + "/" + objectKey;
        }
        try {
            return minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(minioProperties.getPresignedGetExpiryMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned GET URL for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED, "Không thể tạo URL truy cập ảnh.");
        }
    }
}
