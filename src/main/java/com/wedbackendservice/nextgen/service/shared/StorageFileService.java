package com.wedbackendservice.nextgen.service.shared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.UploadFileInput;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.dto.portal.storage.StorageUploadSession;
import com.wedbackendservice.nextgen.entity.Brand;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.BrandRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.text.Normalizer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFileService {

    private static final String UPLOAD_SESSION_PREFIX = "storage:upload:";
    private static final String PENDING_ZSET_KEY = "storage:pending:zset";
    private static final String ORPHAN_SET_KEY = "storage:orphan:set";
    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024; // 10MB
    private static final Duration COMPLETED_SESSION_TTL = Duration.ofHours(2);

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    private final SecurityHelper securityHelper;
    private final BrandRepository brandRepository;
    private final MinioStorageService minioStorageService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Bước 1: Tạo Presigned URL cho Client tải file trực tiếp lên MinIO
     */
    public List<PresignedUploadItemResponse> createPresignedUploads(PresignedUploadRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        if (request.getFiles() == null || request.getFiles().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách tập tin tải lên không được rỗng");
        }

        // Với Brand logo: chỉ cho phép tối đa 1 file mỗi lần
        if (request.getStorageType() == StorageType.BRANDS && request.getFiles().size() > 1) {
            throw new AppException(ErrorCode.STORAGE_LIMIT_EXCEEDED, "Mỗi lần chỉ được tải lên tối đa 1 logo thương hiệu");
        }

        // Validate brandId nếu có
        if (request.getBrandId() != null) {
            brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        }

        int expirySeconds = minioStorageService.getPresignedExpirySeconds();
        String bucket = resolveBucket(request.getStorageType());
        List<PresignedUploadItemResponse> results = new ArrayList<>();

        for (UploadFileInput fileInput : request.getFiles()) {
            validateFileInput(fileInput);

            String uploadId = UUID.randomUUID().toString();
            String safeFileName = sanitizeFileName(fileInput.getFileName());
            String objectKey = buildObjectKey(request.getStorageType(), request.getBrandId(), uploadId, safeFileName);

            String uploadUrl = minioStorageService.generatePresignedUploadUrl(bucket, objectKey, expirySeconds);
            Instant expiresAt = Instant.now().plusSeconds(expirySeconds);

            StorageUploadSession session = StorageUploadSession.builder()
                    .uploadId(uploadId)
                    .storageType(request.getStorageType())
                    .brandId(request.getBrandId())
                    .userId(operator.getUserId())
                    .objectKey(objectKey)
                    .fileName(fileInput.getFileName())
                    .contentType(fileInput.getContentType().toLowerCase().trim())
                    .fileSize(fileInput.getFileSize())
                    .canonicalUrl(minioStorageService.buildCanonicalUrl(bucket, objectKey))
                    .status("PENDING")
                    .createdAt(System.currentTimeMillis())
                    .expiresAt(expiresAt.toEpochMilli())
                    .build();

            saveSession(session, Duration.ofSeconds(expirySeconds));
            redisTemplate.opsForZSet().add(PENDING_ZSET_KEY, uploadId, session.getExpiresAt());

            results.add(PresignedUploadItemResponse.builder()
                    .uploadId(uploadId)
                    .objectKey(objectKey)
                    .uploadUrl(uploadUrl)
                    .expiresAt(expiresAt)
                    .build());
        }

        return results;
    }

    /**
     * Bước 3: Xác nhận upload và kiểm tra tính hợp lệ của file thật trên MinIO
     */
    @Transactional
    public StorageFileResponse confirmUpload(ConfirmUploadRequest request) {
        User operator = securityHelper.authenticateStaffOrAdmin();

        StorageUploadSession session = readSession(request.getUploadId());
        if (session == null) {
            throw new AppException(ErrorCode.STORAGE_SESSION_NOT_FOUND);
        }

        if (!"PENDING".equalsIgnoreCase(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Phiên tải lên không ở trạng thái chờ xác nhận (PENDING)");
        }

        if (!session.getObjectKey().equals(request.getObjectKey())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Thông tin objectKey không khớp với phiên tải lên");
        }

        // Kiểm tra quyền sở hữu phiên
        if (!session.getUserId().equals(operator.getUserId()) && operator.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Bạn không có quyền xác nhận phiên tải lên này");
        }

        String bucket = resolveBucket(session.getStorageType());

        // Kiểm tra file thật trên MinIO (statObject & 16-byte magic bytes)
        StatObjectResponse stat = minioStorageService.statObject(bucket, session.getObjectKey());
        boolean isValidImage = (stat != null) && minioStorageService.validateImageMagicBytes(bucket, session.getObjectKey());

        if (!isValidImage) {
            log.warn("File không tồn tại hoặc sai định dạng magic bytes: bucket={}, key={}", bucket, session.getObjectKey());
            // Dọn dẹp object lỗi trên MinIO
            boolean deleted = minioStorageService.deleteObject(bucket, session.getObjectKey());
            if (!deleted) {
                redisTemplate.opsForSet().add(ORPHAN_SET_KEY, bucket + "/" + session.getObjectKey());
            }
            redisTemplate.opsForZSet().remove(PENDING_ZSET_KEY, session.getUploadId());
            deleteSession(session.getUploadId());
            throw new AppException(ErrorCode.STORAGE_FILE_CORRUPTED);
        }

        // Nhánh A: Có brandId != null (Attach thẳng vào Brand)
        if (session.getBrandId() != null) {
            Brand brand = brandRepository.findById(session.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

            String oldLogoUrl = brand.getLogoUrl();
            brand.setLogoUrl(session.getCanonicalUrl());
            brand.setUpdatedAt(LocalDateTime.now());
            brandRepository.save(brand);

            deleteSession(session.getUploadId());
            redisTemplate.opsForZSet().remove(PENDING_ZSET_KEY, session.getUploadId());

            // Sau commit -> Xóa logo MinIO cũ nếu bị thay thế
            deleteReplacedUrlObjectAfterCommit(bucket, oldLogoUrl, session.getObjectKey());

            String accessUrl = minioStorageService.getAccessUrl(bucket, session.getObjectKey());
            return StorageFileResponse.builder()
                    .uploadId(session.getUploadId())
                    .objectKey(session.getObjectKey())
                    .canonicalUrl(session.getCanonicalUrl())
                    .accessUrl(accessUrl)
                    .brandId(session.getBrandId())
                    .status("ATTACHED")
                    .build();
        }

        // Nhánh B: Chưa có brandId (Upload trước khi Create/Update Brand)
        session.setStatus("COMPLETED");
        saveSession(session, COMPLETED_SESSION_TTL);
        redisTemplate.opsForZSet().remove(PENDING_ZSET_KEY, session.getUploadId());

        String accessUrl = minioStorageService.getAccessUrl(bucket, session.getObjectKey());
        return StorageFileResponse.builder()
                .uploadId(session.getUploadId())
                .objectKey(session.getObjectKey())
                .canonicalUrl(session.getCanonicalUrl())
                .accessUrl(accessUrl)
                .status("COMPLETED")
                .build();
    }

    /**
     * Dùng cho Nhánh B: Reserve logo khi tạo mới hoặc cập nhật Brand
     */
    public String reserveInputBrandLogo(String logoUploadId, String oldLogoUrl) {
        if (logoUploadId == null || logoUploadId.isBlank()) {
            return null;
        }

        StorageUploadSession session = readSession(logoUploadId.trim());
        if (session == null) {
            // Có thể session đã được attach trực tiếp vào Brand ở bước confirmUpload (Nhánh A)
            log.info("Phiên upload {} không còn trong Redis (đã được attach hoặc đã hoàn tất)", logoUploadId);
            return null;
        }

        if (!"COMPLETED".equalsIgnoreCase(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Phiên tải lên logo chưa được xác nhận hoàn tất");
        }

        if (session.getStorageType() != StorageType.BRANDS) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Phiên tải lên không phải loại thương hiệu");
        }

        String bucket = resolveBucket(StorageType.BRANDS);

        // Sau commit -> xóa session Redis đã dùng & xóa logo MinIO cũ nếu bị thay thế
        runAfterCommit(() -> {
            deleteSession(logoUploadId.trim());
            deleteReplacedUrlObjectAfterCommit(bucket, oldLogoUrl, session.getObjectKey());
        });

        return session.getCanonicalUrl();
    }

    /**
     * Chuẩn hóa URL ảnh (chuyển đổi Presigned GET URL tạm thời về Canonical URL lưu trong database)
     */
    public String toCanonicalUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String bucket = resolveBucket(StorageType.BRANDS);
        String objectKey = minioStorageService.extractObjectKey(bucket, url);
        if (objectKey != null && !objectKey.isBlank()) {
            return minioStorageService.buildCanonicalUrl(bucket, objectKey);
        }
        return url.trim();
    }

    /**
     * Xóa object MinIO của Brand sau khi commit transaction (Dùng khi DELETE /brands/{id} hoặc DELETE /brands/{id}/logo)
     */
    public void deleteBrandLogoObjectAfterCommit(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        String bucket = resolveBucket(StorageType.BRANDS);
        String objectKey = minioStorageService.extractObjectKey(bucket, logoUrl);
        if (objectKey != null && !objectKey.isBlank()) {
            runAfterCommit(() -> {
                boolean ok = minioStorageService.deleteObject(bucket, objectKey);
                if (!ok) {
                    redisTemplate.opsForSet().add(ORPHAN_SET_KEY, bucket + "/" + objectKey);
                }
            });
        }
    }

    /**
     * Resolve URL truy cập cho Brand logo: Nếu là ảnh MinIO thì lấy accessUrl, ngược lại giữ nguyên URL legacy
     */
    public String resolveAccessUrl(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return logoUrl;
        }
        String bucket = resolveBucket(StorageType.BRANDS);
        String objectKey = minioStorageService.extractObjectKey(bucket, logoUrl);
        if (objectKey != null && !objectKey.isBlank()) {
            return minioStorageService.getAccessUrl(bucket, objectKey);
        }
        return logoUrl;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private void deleteReplacedUrlObjectAfterCommit(String bucket, String oldLogoUrl, String newObjectKey) {
        if (oldLogoUrl == null || oldLogoUrl.isBlank()) {
            return;
        }
        String oldObjectKey = minioStorageService.extractObjectKey(bucket, oldLogoUrl);
        if (oldObjectKey != null && !oldObjectKey.equalsIgnoreCase(newObjectKey)) {
            runAfterCommit(() -> {
                boolean ok = minioStorageService.deleteObject(bucket, oldObjectKey);
                if (!ok) {
                    redisTemplate.opsForSet().add(ORPHAN_SET_KEY, bucket + "/" + oldObjectKey);
                }
            });
        }
    }

    private void runAfterCommit(Runnable runnable) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        runnable.run();
                    } catch (Exception e) {
                        log.error("Lỗi khi thực thi tác vụ sau commit", e);
                    }
                }
            });
        } else {
            runnable.run();
        }
    }

    private void validateFileInput(UploadFileInput fileInput) {
        if (fileInput.getFileSize() == null || fileInput.getFileSize() <= 0) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Kích thước tập tin không hợp lệ");
        }
        if (fileInput.getFileSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.STORAGE_FILE_SIZE_EXCEEDED);
        }
        String contentType = fileInput.getContentType() != null ? fileInput.getContentType().toLowerCase().trim() : "";
        if (!ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE_TYPE);
        }
    }

    private String resolveBucket(StorageType storageType) {
        if (storageType == StorageType.BRANDS) {
            return minioStorageService.getBrandsBucket();
        }
        return minioStorageService.getBrandsBucket();
    }

    private String buildObjectKey(StorageType storageType, Long brandId, String uploadId, String fileName) {
        String shortId = uploadId.replace("-", "").substring(0, 8);
        if (storageType == StorageType.BRANDS) {
            if (brandId != null) {
                return "brands/" + brandId + "/" + shortId + "_" + fileName;
            }
            return "brands/" + shortId + "_" + fileName;
        }
        return storageType.name().toLowerCase() + "/" + shortId + "_" + fileName;
    }

    private String sanitizeFileName(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            return "file.bin";
        }
        String name = originalFileName.trim().replaceAll("[\\\\/:*?\"<>|\\s]", "-");
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        return Pattern.compile("[^\\p{ASCII}a-zA-Z0-9.-]").matcher(normalized).replaceAll("");
    }

    private void saveSession(StorageUploadSession session, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(session);
            redisTemplate.opsForValue().set(UPLOAD_SESSION_PREFIX + session.getUploadId(), json, ttl);
        } catch (Exception e) {
            log.error("Failed to save storage session to Redis", e);
            throw new AppException(ErrorCode.STORAGE_ERROR, "Không thể lưu phiên tải lên: " + e.getMessage());
        }
    }

    private StorageUploadSession readSession(String uploadId) {
        try {
            String json = redisTemplate.opsForValue().get(UPLOAD_SESSION_PREFIX + uploadId);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, StorageUploadSession.class);
        } catch (Exception e) {
            log.error("Failed to parse storage session from Redis", e);
            return null;
        }
    }

    private void deleteSession(String uploadId) {
        redisTemplate.delete(UPLOAD_SESSION_PREFIX + uploadId);
    }
}
