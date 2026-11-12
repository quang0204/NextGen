package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    @org.springframework.beans.factory.annotation.Autowired
    public MinioStorageService(
            @Qualifier("minioInternalClient") MinioClient minioInternalClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient,
            @org.springframework.beans.factory.annotation.Autowired(required = false) MinioProperties minioProperties) {
        this.minioInternalClient = minioInternalClient;
        this.minioPublicClient = minioPublicClient;
        this.minioProperties = (minioProperties != null) ? minioProperties : new MinioProperties();
    }

    public MinioStorageService(MinioClient minioInternalClient, MinioClient minioPublicClient) {
        this(minioInternalClient, minioPublicClient, new MinioProperties());
    }

    public record ObjectMetadata(long size, String etag, String contentType) {}

    public String getBrandsBucket() {
        return minioProperties.getBrandsBucket();
    }

    public String getCategoriesBucket() {
        return minioProperties.getCategoriesBucket();
    }

    public int getPresignedExpirySeconds() {
        return minioProperties.getPresignedExpirySeconds();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initDefaultBuckets() {
        try {
            ensureBucketReady(minioProperties.getBucket().getProducts());
            ensureBucketReady(minioProperties.getBrandsBucket());
            ensureBucketReady(minioProperties.getCategoriesBucket());
        } catch (Exception e) {
            log.warn("MinIO chưa sẵn sàng khi khởi động ({}). Sẽ tự khởi tạo bucket ở lần upload đầu tiên.", e.getMessage());
        }
    }

    public void ensureBucketReady(String bucket) {
        if (bucket == null || bucket.isBlank() || initializedBuckets.contains(bucket) || minioInternalClient == null) {
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
            GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expirySeconds, TimeUnit.SECONDS);

            if (contentType != null && !contentType.isBlank()) {
                builder.extraHeaders(Map.of("Content-Type", contentType));
            }

            return minioPublicClient.getPresignedObjectUrl(builder.build());
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for {}/{}: {}", bucket, objectKey, e.getMessage(), e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED,
                    "Không thể tạo URL tải ảnh lên MinIO: " + e.getMessage());
        }
    }

    public String generatePresignedUploadUrl(String bucket, String objectKey, int expirySeconds) {
        return generatePresignedUploadUrl(bucket, objectKey, null, expirySeconds);
    }

    public String generatePresignedGetUrl(String bucket, String objectKey, int expirySeconds) {
        try {
            return minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned GET url for bucket: {}, object: {}", bucket, objectKey, e);
            throw new AppException(ErrorCode.STORAGE_OPERATION_FAILED,
                    "Không thể tạo liên kết truy cập MinIO: " + e.getMessage());
        }
    }

    public String getAccessUrl(String bucket, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String publicBase = minioProperties.getPublicBaseUrl();
        if (publicBase != null && !publicBase.isBlank()) {
            String base = publicBase.endsWith("/") ? publicBase.substring(0, publicBase.length() - 1) : publicBase;
            return base + "/" + objectKey;
        }
        if (minioProperties.isPublicRead()) {
            String base = (minioProperties.getPublicEndpoint() != null && !minioProperties.getPublicEndpoint().isBlank())
                    ? minioProperties.getPublicEndpoint()
                    : minioProperties.getEndpoint();
            base = base.replaceAll("/+$", "");
            return base + "/" + bucket + "/" + objectKey;
        }
        return generatePresignedGetUrl(bucket, objectKey, 86400);
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

    public boolean validateImageMagicBytes(String bucket, String objectKey) {
        try {
            byte[] header = readObjectHeader(bucket, objectKey, 32);
            if (header == null || header.length < 4) {
                return false;
            }

            // 1. JPEG (FF D8 FF)
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return true;
            }

            // 2. PNG (89 50 4E 47 0D 0A 1A 0A)
            if (header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 'P'
                    && header[2] == 'N'
                    && header[3] == 'G'
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A) {
                return true;
            }

            // 3. GIF (GIF87a hoặc GIF89a)
            if (header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8') {
                return true;
            }

            // 4. WEBP (RIFF....WEBP)
            if (header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return true;
            }

            // 5. SVG (Chuỗi xml / svg)
            String snippet = new String(header, 0, header.length, StandardCharsets.UTF_8).toLowerCase();
            if (snippet.contains("<svg") || snippet.contains("<?xml")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to read magic bytes for bucket: {}, object: {}", bucket, objectKey, e);
            return false;
        }
    }

    public boolean deleteObject(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank() || objectKey == null || objectKey.isBlank()) {
            return true;
        }
        try {
            minioInternalClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted MinIO object successfully: bucket={}, object={}", bucket, objectKey);
            return true;
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}/{}: {}", bucket, objectKey, e.getMessage());
            return false;
        }
    }

    public String extractObjectKey(String bucket, String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String cleanUrl = url.trim();

        // 1. Dạng Canonical: minio://{bucket}/{objectKey}
        String canonicalPrefix = "minio://" + bucket + "/";
        if (cleanUrl.startsWith(canonicalPrefix)) {
            return cleanUrl.substring(canonicalPrefix.length());
        }

        // 2. Dạng trực tiếp: {bucket}/{objectKey}
        String directPrefix = bucket + "/";
        if (cleanUrl.startsWith(directPrefix)) {
            return cleanUrl.substring(directPrefix.length());
        }

        // 3. Dạng HTTP URL chứa /{bucket}/...
        String slashBucketSlash = "/" + bucket + "/";
        int idx = cleanUrl.indexOf(slashBucketSlash);
        if (idx != -1) {
            String pathAndQuery = cleanUrl.substring(idx + slashBucketSlash.length());
            int queryIdx = pathAndQuery.indexOf('?');
            return queryIdx != -1 ? pathAndQuery.substring(0, queryIdx) : pathAndQuery;
        }

        // Không khớp prefix MinIO brands (URL ngoại legacy)
        return null;
    }

    public String buildCanonicalUrl(String bucket, String objectKey) {
        return "minio://" + bucket + "/" + objectKey;
    }
}
