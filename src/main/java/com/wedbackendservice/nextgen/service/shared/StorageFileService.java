package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductImage;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.time.Duration;
import java.util.*;
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

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif"
    );

    private final MinioStorageService minioStorageService;
    private final TokenRedisService tokenRedisService;
    private final MinioProperties minioProperties;
    private final SecurityHelper securityHelper;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public List<PresignedUploadItemResponse> generatePresignedUrls(PresignedUploadRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();

        String storageType = (request.getStorageType() == null || request.getStorageType().isBlank())
                ? "PRODUCT"
                : request.getStorageType().trim().toUpperCase();
        if (!"PRODUCT".equals(storageType)) {
            throw new AppException(ErrorCode.STORAGE_UNSUPPORTED_TYPE,
                    "storageType không được hỗ trợ. Hiện chỉ hỗ trợ: PRODUCT.");
        }

        if (request.getProductId() != null && !productRepository.existsById(request.getProductId())) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                    "Không tìm thấy sản phẩm có ID " + request.getProductId() + "!");
        }

        List<PresignedUploadRequest.FileItem> files = request.getFiles();
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách tệp tải lên không được để trống.");
        }

        int maxFiles = minioProperties.getMaxFilesPerProduct();
        if (files.size() > maxFiles) {
            throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED,
                    "Tối đa chỉ được tải lên " + maxFiles + " ảnh mỗi lần.");
        }

        String bucket = minioProperties.getBucket().getProducts();
        int expirySeconds = Math.max(60, minioProperties.getPresignedUploadExpiryMinutes() * 60);
        long now = System.currentTimeMillis();
        long expiresAt = now + expirySeconds * 1000L;

        List<PresignedUploadItemResponse> responses = new ArrayList<>(files.size());
        for (PresignedUploadRequest.FileItem file : files) {
            String contentType = normalizeMimeType(file.getContentType());
            if (!ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                        "Định dạng ảnh không hợp lệ (" + file.getContentType() + "). Chỉ chấp nhận JPEG, PNG, WEBP, GIF.");
            }

            long maxBytes = minioProperties.getMaxFileSizeBytes();
            if (file.getFileSize() == null || file.getFileSize() <= 0 || file.getFileSize() > maxBytes) {
                throw new AppException(ErrorCode.STORAGE_FILE_TOO_LARGE,
                        "Kích thước tệp '" + file.getFileName() + "' vượt quá giới hạn " + (maxBytes / (1024 * 1024)) + "MB.");
            }

            String uploadId = UUID.randomUUID().toString();
            String safeFileName = sanitizeFileName(file.getFileName(), contentType);
            String folder = request.getProductId() != null
                    ? String.valueOf(request.getProductId())
                    : "temp/" + currentUser.getUserId();
            String objectKey = folder + "/" + uploadId + "-" + safeFileName;

            String uploadUrl = minioStorageService.generatePresignedUploadUrl(
                    bucket, objectKey, contentType, expirySeconds);

            UploadSessionData sessionData = UploadSessionData.builder()
                    .uploadId(uploadId)
                    .ownerId(currentUser.getUserId())
                    .productId(request.getProductId())
                    .storageType(storageType)
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .originalFileName(file.getFileName().trim())
                    .contentType(contentType)
                    .expectedSize(file.getFileSize())
                    .status(UploadSessionData.STATUS_PENDING)
                    .presignedExpiresAtMillis(expiresAt)
                    .createdAtMillis(now)
                    .build();

            tokenRedisService.saveUploadSession(uploadId, sessionData, Duration.ofSeconds(expirySeconds + 60L));
            tokenRedisService.addPendingUpload(uploadId, expiresAt);

            responses.add(PresignedUploadItemResponse.builder()
                    .uploadId(uploadId)
                    .uploadUrl(uploadUrl)
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .originalFileName(file.getFileName().trim())
                    .contentType(contentType)
                    .fileSize(file.getFileSize())
                    .expiresAt(expiresAt)
                    .expiresInSeconds(expirySeconds)
                    .requiredHeaders(Map.of("Content-Type", contentType))
                    .build());
        }

        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse confirmUpload(ConfirmUploadRequest request) {
        User currentUser = securityHelper.authenticateStaffOrAdmin();
        String uploadId = request.getUploadId().trim();
        String requestedObjectKey = request.getObjectKey().trim();

        UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
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

        if (!UploadSessionData.STATUS_PENDING.equals(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                    "Phiên tải ảnh đã được xử lý hoặc không ở trạng thái PENDING.");
        }

        if (!Objects.equals(session.getOwnerId(), currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED,
                    "Bạn không có quyền xác nhận phiên tải ảnh này.");
        }

        if (!Objects.equals(session.getObjectKey(), requestedObjectKey)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "objectKey không khớp với phiên tải ảnh.");
        }

        if (session.getPresignedExpiresAtMillis() == null
                || session.getPresignedExpiresAtMillis() < System.currentTimeMillis()) {
            tokenRedisService.removePendingUpload(uploadId);
            tokenRedisService.deleteUploadSession(uploadId);
            minioStorageService.deleteObject(session.getBucket(), session.getObjectKey());
            throw new AppException(ErrorCode.STORAGE_UPLOAD_EXPIRED);
        }

        Optional<MinioStorageService.ObjectMetadata> statOpt =
                minioStorageService.statObject(session.getBucket(), session.getObjectKey());
        if (statOpt.isEmpty()) {
            tokenRedisService.removePendingUpload(uploadId);
            tokenRedisService.deleteUploadSession(uploadId);
            throw new AppException(ErrorCode.STORAGE_OBJECT_NOT_UPLOADED);
        }

        MinioStorageService.ObjectMetadata stat = statOpt.get();
        if (stat.size() <= 0 || stat.size() > minioProperties.getMaxFileSizeBytes()) {
            minioStorageService.deleteObject(session.getBucket(), session.getObjectKey());
            tokenRedisService.removePendingUpload(uploadId);
            tokenRedisService.deleteUploadSession(uploadId);
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                    "Kích thước tệp thực tế trên MinIO không hợp lệ.");
        }

        byte[] header = minioStorageService.readObjectHeader(session.getBucket(), session.getObjectKey(), 16);
        if (!isValidImageMagicBytes(header, session.getContentType())) {
            minioStorageService.deleteObject(session.getBucket(), session.getObjectKey());
            tokenRedisService.removePendingUpload(uploadId);
            tokenRedisService.deleteUploadSession(uploadId);
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                    "Tệp tải lên không khớp định dạng ảnh hợp lệ (sai magic bytes).");
        }

        tokenRedisService.removePendingUpload(uploadId);
        session.setStatus(UploadSessionData.STATUS_COMPLETED);
        session.setActualSize(stat.size());
        session.setEtag(stat.etag());

        String imageUrl = minioStorageService.getFileUrl(session.getBucket(), session.getObjectKey());
        boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());
        Long savedImageId = null;
        Integer resolvedOrder = request.getSortOrder();

        if (session.getProductId() != null) {
            Product product = productRepository.findById(session.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                            "Không tìm thấy sản phẩm có ID " + session.getProductId() + "!"));

            if (isPrimary) {
                productImageRepository.clearPrimaryByProductId(product.getProductId());
            }
            if (resolvedOrder == null) {
                resolvedOrder = (int) productImageRepository.countByProduct_ProductId(product.getProductId()) + 1;
            }

            ProductImage savedImage = productImageRepository.save(ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrl)
                    .bucket(session.getBucket())
                    .objectKey(session.getObjectKey())
                    .originalFileName(session.getOriginalFileName())
                    .contentType(session.getContentType())
                    .fileSize(stat.size())
                    .etag(stat.etag())
                    .isPrimary(isPrimary)
                    .displayOrder(resolvedOrder)
                    .build());
            savedImageId = savedImage.getImageId();

            if (isPrimary || product.getThumbnailUrl() == null || product.getThumbnailUrl().isBlank()) {
                product.setThumbnailUrl(imageUrl);
                productRepository.save(product);
            }

            runAfterCommit(() -> tokenRedisService.deleteUploadSession(uploadId));
        } else {
            long ttlHours = Math.max(1, minioProperties.getCompletedSessionTtlHours());
            tokenRedisService.saveUploadSession(uploadId, session, Duration.ofHours(ttlHours));
        }

        return StorageFileResponse.builder()
                .uploadId(uploadId)
                .imageId(savedImageId)
                .productId(session.getProductId())
                .imageUrl(imageUrl)
                .bucket(session.getBucket())
                .objectKey(session.getObjectKey())
                .originalFileName(session.getOriginalFileName())
                .contentType(session.getContentType())
                .fileSize(stat.size())
                .etag(stat.etag())
                .isPrimary(isPrimary)
                .sortOrder(resolvedOrder)
                .status(UploadSessionData.STATUS_COMPLETED)
                .build();
    }

    public int cleanupExpiredPendingUploads() {
        Set<String> expiredIds = tokenRedisService.getExpiredPendingUploadIds(System.currentTimeMillis(), 100);
        if (expiredIds == null || expiredIds.isEmpty()) {
            return 0;
        }
        int cleaned = 0;
        for (String uploadId : expiredIds) {
            try {
                UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
                if (session == null) {
                    tokenRedisService.removePendingUpload(uploadId);
                    cleaned++;
                    continue;
                }
                if (UploadSessionData.STATUS_PENDING.equals(session.getStatus())) {
                    minioStorageService.deleteObject(session.getBucket(), session.getObjectKey());
                    tokenRedisService.deleteUploadSession(uploadId);
                }
                tokenRedisService.removePendingUpload(uploadId);
                cleaned++;
            } catch (Exception e) {
                log.warn("Failed to cleanup expired upload session {}: {}", uploadId, e.getMessage());
            }
        }
        return cleaned;
    }

    public static boolean isValidImageMagicBytes(byte[] header, String expectedContentType) {
        if (header == null || expectedContentType == null) {
            return false;
        }
        String normalized = normalizeMimeType(expectedContentType);
        return switch (normalized) {
            case "image/jpeg" -> header.length >= 3
                    && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8
                    && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> header.length >= 8
                    && (header[0] & 0xFF) == 0x89
                    && header[1] == 0x50
                    && header[2] == 0x4E
                    && header[3] == 0x47
                    && header[4] == 0x0D
                    && header[5] == 0x0A
                    && header[6] == 0x1A
                    && header[7] == 0x0A;
            case "image/gif" -> header.length >= 6
                    && header[0] == 'G'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9')
                    && header[5] == 'a';
            case "image/webp" -> header.length >= 12
                    && header[0] == 'R'
                    && header[1] == 'I'
                    && header[2] == 'F'
                    && header[3] == 'F'
                    && header[8] == 'W'
                    && header[9] == 'E'
                    && header[10] == 'B'
                    && header[11] == 'P';
            default -> false;
        };
    }

    public static String normalizeMimeType(String contentType) {
        if (contentType == null) {
            return "";
        }
        String clean = contentType.trim().toLowerCase(Locale.ROOT);
        int semiIdx = clean.indexOf(';');
        if (semiIdx >= 0) {
            clean = clean.substring(0, semiIdx).trim();
        }
        if ("image/jpg".equals(clean)) {
            return "image/jpeg";
        }
        return clean;
    }

    private String sanitizeFileName(String originalName, String contentType) {
        String name = originalName != null ? originalName.trim() : "image";
        int lastSlash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }
        name = name.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.isBlank() || ".".equals(name) || "..".equals(name)) {
            name = "image." + MIME_TO_EXT.getOrDefault(contentType, "bin");
        }
        if (!name.contains(".")) {
            name = name + "." + MIME_TO_EXT.getOrDefault(contentType, "bin");
        }
        if (name.length() > 120) {
            name = name.substring(name.length() - 120);
        }
        return name;
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
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
