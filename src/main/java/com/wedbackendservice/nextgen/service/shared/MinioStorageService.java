package com.wedbackendservice.nextgen.service.shared;

import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class MinioStorageService {

    private final MinioClient minioClient;
    private final MinioClient minioPublicClient;

    @Value("${minio.brands-bucket:brands}")
    private String brandsBucket;

    @Value("${minio.presigned-expiry-seconds:900}")
    private int presignedExpirySeconds;

    @Value("${minio.public-base-url:}")
    private String publicBaseUrl;

    public MinioStorageService(
            @Qualifier("minioClient") MinioClient minioClient,
            @Qualifier("minioPublicClient") MinioClient minioPublicClient) {
        this.minioClient = minioClient;
        this.minioPublicClient = minioPublicClient;
    }

    public String getBrandsBucket() {
        return brandsBucket;
    }

    public int getPresignedExpirySeconds() {
        return presignedExpirySeconds;
    }

    /**
     * Tạo Presigned PUT URL để client trực tiếp upload file
     */
    public String generatePresignedUploadUrl(String bucket, String objectKey, int expirySeconds) {
        try {
            return minioPublicClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(expirySeconds, TimeUnit.SECONDS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned PUT url for bucket: {}, object: {}", bucket, objectKey, e);
            throw new RuntimeException("Không thể tạo liên kết tải lên MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Tạo Presigned GET URL để client tải hoặc xem file
     */
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
            throw new RuntimeException("Không thể tạo liên kết truy cập MinIO: " + e.getMessage(), e);
        }
    }

    /**
     * Lấy URL truy cập (Access URL): Ưu tiên public base URL nếu có, ngược lại sinh presigned GET URL
     */
    public String getAccessUrl(String bucket, String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        if (publicBaseUrl != null && !publicBaseUrl.isBlank()) {
            String base = publicBaseUrl.endsWith("/") ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
            return base + "/" + objectKey;
        }
        // Mặc định sinh presigned GET với thời hạn 24 giờ (86400s)
        return generatePresignedGetUrl(bucket, objectKey, 86400);
    }

    /**
     * Kiểm tra trạng thái và kích thước object trên MinIO
     */
    public StatObjectResponse statObject(String bucket, String objectKey) {
        try {
            return minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Stat object failed for bucket: {}, object: {}: {}", bucket, objectKey, e.getMessage());
            return null;
        }
    }

    /**
     * Đọc 16 bytes đầu (Magic bytes) và xác thực định dạng ảnh (JPEG, PNG, WEBP, GIF, SVG)
     */
    public boolean validateImageMagicBytes(String bucket, String objectKey) {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .offset(0L)
                        .length(32L) // Đọc 32 bytes đầu để bao quát cả SVG và WEBP
                        .build())) {
            byte[] header = new byte[32];
            int readCount = stream.read(header);
            if (readCount < 4) {
                return false;
            }

            // 1. JPEG (FF D8 FF)
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return true;
            }

            // 2. PNG (89 50 4E 47 0D 0A 1A 0A)
            if (readCount >= 8
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
            if (readCount >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P') {
                return true;
            }

            // 5. SVG (Chuỗi xml / svg)
            String snippet = new String(header, 0, readCount, StandardCharsets.UTF_8).toLowerCase();
            if (snippet.contains("<svg") || snippet.contains("<?xml")) {
                return true;
            }

            return false;
        } catch (Exception e) {
            log.error("Failed to read magic bytes for bucket: {}, object: {}", bucket, objectKey, e);
            return false;
        }
    }

    /**
     * Xóa object khỏi MinIO
     */
    public boolean deleteObject(String bucket, String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted MinIO object successfully: bucket={}, object={}", bucket, objectKey);
            return true;
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO: bucket={}, object={}", bucket, objectKey, e);
            return false;
        }
    }

    /**
     * Trích xuất objectKey từ URL lưu trong DB (hoặc URL trả về từ MinIO).
     * Nhánh Flowchart: Khớp prefix MinIO brands vs Không khớp (URL ngoại legacy)
     */
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

    /**
     * Xây dựng canonical URL chuẩn lưu vào database
     */
    public String buildCanonicalUrl(String bucket, String objectKey) {
        return "minio://" + bucket + "/" + objectKey;
    }
}
