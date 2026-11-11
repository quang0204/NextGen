package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.dto.shared.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.shared.storage.PresignedUploadBatchRequest;
import com.wedbackendservice.nextgen.dto.shared.storage.PresignedUploadBatchResponse;
import com.wedbackendservice.nextgen.dto.shared.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductImage;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class StorageFileService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp", "image/gif"
    );
    private static final Duration COMPLETED_SESSION_TTL = Duration.ofHours(2);

    private final StorageService storageService;
    private final TokenRedisService tokenRedisService;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final SecurityHelper securityHelper;
    private final MinioProperties minioProperties;

    public PresignedUploadBatchResponse createPresignedUploads(PresignedUploadBatchRequest request) {
        User currentUser = securityHelper.getCurrentUser();
        StorageType storageType = request.getStorageType() != null ? request.getStorageType() : StorageType.PRODUCTS;

        if (storageType != StorageType.AVATARS || request.getProductId() != null) {
            requireStaffOrAdmin(currentUser);
        }

        if (request.getProductId() != null) {
            Product product = productRepository.findById(request.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
            if ("DISCONTINUED".equalsIgnoreCase(product.getStatus())) {
                throw new AppException(ErrorCode.PRODUCT_INACTIVE, "Sản phẩm đã ngừng kinh doanh, không thể thêm ảnh.");
            }
            long currentImages = productImageRepository.countByProduct_ProductId(request.getProductId());
            if (currentImages + request.getFiles().size() > minioProperties.getMaxFilesPerProduct()) {
                throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
            }
        }

        long maxSize = minioProperties.getMaxFileSizeBytes();
        List<ValidatedFile> validatedFiles = new ArrayList<>(request.getFiles().size());
        for (PresignedUploadBatchRequest.FileUploadItem item : request.getFiles()) {
            String normalizedContentType = normalizeAndValidateContentType(item.getContentType());
            if (item.getFileSize() == null || item.getFileSize() <= 0 || item.getFileSize() > maxSize) {
                throw new AppException(ErrorCode.STORAGE_FILE_TOO_LARGE,
                        "Kích thước ảnh phải từ 1 byte đến " + (maxSize / (1024 * 1024)) + "MB.");
            }
            validatedFiles.add(new ValidatedFile(sanitizeFileName(item.getFileName()), normalizedContentType, item.getFileSize()));
        }

        long putExpirySeconds = minioProperties.getPresignPutExpirationSeconds();
        List<PresignedUploadBatchResponse.PresignedUploadItemResponse> responses = new ArrayList<>(validatedFiles.size());
        for (ValidatedFile file : validatedFiles) {
            String uploadId = UUID.randomUUID().toString();
            String prefix = request.getProductId() != null
                    ? String.valueOf(request.getProductId())
                    : LocalDate.now().toString();
            String objectKey = String.format("%s/%s-%s", prefix, uploadId.substring(0, 8), file.fileName());

            StorageService.PresignedUploadInfo presigned = storageService.generatePresignedUploadUrl(
                    storageType, objectKey, file.contentType(), Duration.ofSeconds(putExpirySeconds));

            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(putExpirySeconds);
            UploadSessionData session = UploadSessionData.builder()
                    .uploadId(uploadId)
                    .ownerId(currentUser.getUserId())
                    .productId(request.getProductId())
                    .storageType(storageType)
                    .storageProvider(presigned.provider())
                    .bucketName(presigned.bucketName())
                    .objectKey(presigned.objectKey())
                    .originalFileName(file.fileName())
                    .contentType(file.contentType())
                    .expectedFileSize(file.fileSize())
                    .status(UploadStatus.PENDING)
                    .expiresAtEpochMs(expiresAt.toEpochMilli())
                    .createdAtEpochMs(now.toEpochMilli())
                    .build();

            tokenRedisService.saveUploadSession(uploadId, session, Duration.ofSeconds(putExpirySeconds + 60));
            tokenRedisService.trackPendingUpload(uploadId, presigned.bucketName(), presigned.objectKey(), expiresAt.toEpochMilli());

            responses.add(PresignedUploadBatchResponse.PresignedUploadItemResponse.builder()
                    .uploadId(uploadId)
                    .provider(presigned.provider())
                    .storageType(storageType)
                    .bucketName(presigned.bucketName())
                    .objectKey(presigned.objectKey())
                    .uploadUrl(presigned.uploadUrl())
                    .httpMethod("PUT")
                    .expiration(expiresAt.toString())
                    .expiresInSeconds(presigned.expiresInSeconds())
                    .requiredHeaders(Map.of("Content-Type", file.contentType()))
                    .build());
        }

        return PresignedUploadBatchResponse.builder().items(responses).build();
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse confirmUpload(ConfirmUploadRequest request) {
        User currentUser = securityHelper.getCurrentUser();
        UploadSessionData session = tokenRedisService.getUploadSession(request.getUploadId().trim());
        if (session == null) {
            throw new AppException(ErrorCode.STORAGE_UPLOAD_SESSION_EXPIRED);
        }

        if (!isStaffOrAdmin(currentUser) && !Objects.equals(session.getOwnerId(), currentUser.getUserId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        if (session.getStatus() != UploadStatus.PENDING) {
            throw new AppException(ErrorCode.STORAGE_UPLOAD_ALREADY_CONFIRMED);
        }
        if (request.getObjectKey() != null && !request.getObjectKey().isBlank()
                && !request.getObjectKey().trim().equals(session.getObjectKey())) {
            throw new AppException(ErrorCode.STORAGE_OBJECT_KEY_MISMATCH);
        }
        if (System.currentTimeMillis() > session.getExpiresAtEpochMs()) {
            cleanupFailedSession(session);
            throw new AppException(ErrorCode.STORAGE_UPLOAD_SESSION_EXPIRED);
        }

        StorageService.StoredObjectMeta meta = storageService
                .getObjectMetadata(session.getBucketName(), session.getObjectKey())
                .orElseThrow(() -> new AppException(ErrorCode.STORAGE_OBJECT_NOT_UPLOADED));

        if (meta.contentLength() <= 0 || meta.contentLength() > minioProperties.getMaxFileSizeBytes()) {
            cleanupInvalidUploadedObject(session);
            throw new AppException(ErrorCode.STORAGE_FILE_TOO_LARGE);
        }
        if (meta.contentType() != null && !meta.contentType().isBlank()
                && !ALLOWED_CONTENT_TYPES.contains(meta.contentType().trim().toLowerCase(Locale.ROOT))) {
            cleanupInvalidUploadedObject(session);
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE);
        }

        byte[] headerBytes = storageService.readObjectHeader(session.getBucketName(), session.getObjectKey(), 16);
        if (!isValidImageMagicBytes(headerBytes, session.getContentType())) {
            cleanupInvalidUploadedObject(session);
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                    "Nội dung tệp thực tế không phải định dạng ảnh hợp lệ.");
        }

        session.setStatus(UploadStatus.COMPLETED);
        session.setActualFileSize(meta.contentLength());
        session.setETag(meta.eTag());
        if (meta.contentType() != null && !meta.contentType().isBlank()) {
            session.setContentType(meta.contentType().trim().toLowerCase(Locale.ROOT));
        }

        tokenRedisService.untrackPendingUpload(session.getUploadId(), session.getBucketName(), session.getObjectKey());

        Long savedImageId = null;
        if (session.getProductId() != null) {
            savedImageId = attachImageToProduct(session, request);
            tokenRedisService.deleteUploadSession(session.getUploadId());
        } else {
            tokenRedisService.saveUploadSession(session.getUploadId(), session, COMPLETED_SESSION_TTL);
        }

        return StorageFileResponse.builder()
                .uploadId(session.getUploadId())
                .imageId(savedImageId)
                .productId(session.getProductId())
                .provider(session.getStorageProvider())
                .storageType(session.getStorageType())
                .bucketName(session.getBucketName())
                .objectKey(session.getObjectKey())
                .imageUrl(storageService.getAccessUrl(session.getBucketName(), session.getObjectKey()))
                .originalFileName(session.getOriginalFileName())
                .contentType(session.getContentType())
                .fileSize(session.getActualFileSize())
                .eTag(session.getETag())
                .status(UploadStatus.COMPLETED)
                .build();
    }

    private Long attachImageToProduct(UploadSessionData session, ConfirmUploadRequest request) {
        Product product = productRepository.findById(session.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        long currentCount = productImageRepository.countByProduct_ProductId(product.getProductId());
        if (currentCount >= minioProperties.getMaxFilesPerProduct()) {
            throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
        }

        String canonicalUrl = storageService.getObjectUrl(session.getBucketName(), session.getObjectKey());
        boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());
        int displayOrder = request.getSortOrder() != null ? request.getSortOrder() : (int) currentCount + 1;

        ProductImage saved = productImageRepository.save(ProductImage.builder()
                .product(product)
                .imageUrl(canonicalUrl)
                .storageProvider(session.getStorageProvider())
                .storageType(session.getStorageType())
                .bucket(session.getBucketName())
                .objectKey(session.getObjectKey())
                .originalFileName(session.getOriginalFileName())
                .contentType(session.getContentType())
                .fileSize(session.getActualFileSize())
                .eTag(session.getETag())
                .isPrimary(isPrimary)
                .displayOrder(displayOrder)
                .build());

        if (isPrimary || product.getThumbnailUrl() == null || product.getThumbnailUrl().isBlank()) {
            product.setThumbnailUrl(canonicalUrl);
            productRepository.save(product);
        }
        return saved.getImageId();
    }

    private void cleanupFailedSession(UploadSessionData session) {
        tokenRedisService.untrackPendingUpload(session.getUploadId(), session.getBucketName(), session.getObjectKey());
        tokenRedisService.deleteUploadSession(session.getUploadId());
    }

    private void cleanupInvalidUploadedObject(UploadSessionData session) {
        try {
            storageService.deleteObject(session.getBucketName(), session.getObjectKey());
        } catch (Exception e) {
            log.warn("Không thể xóa file không hợp lệ key={}: {}", session.getObjectKey(), e.getMessage());
            tokenRedisService.addOrphanObject(session.getBucketName(), session.getObjectKey());
        }
        cleanupFailedSession(session);
    }

    private boolean isStaffOrAdmin(User user) {
        return user.getRole() == Role.ADMIN || user.getRole() == Role.STAFF;
    }

    private void requireStaffOrAdmin(User user) {
        if (!isStaffOrAdmin(user)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
    }

    private String normalizeAndValidateContentType(String contentType) {
        if (contentType == null) {
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE);
        }
        String normalized = contentType.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_CONTENT_TYPES.contains(normalized)) {
            throw new AppException(ErrorCode.STORAGE_INVALID_FILE);
        }
        return normalized;
    }

    private String sanitizeFileName(String fileName) {
        String baseName = fileName.replace("\\", "/");
        int lastSlash = baseName.lastIndexOf('/');
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }
        String cleaned = baseName.trim().replaceAll("[^a-zA-Z0-9._-]", "-");
        if (cleaned.isBlank() || cleaned.equals(".") || cleaned.equals("..")) {
            return "image";
        }
        return cleaned.length() > 100 ? cleaned.substring(cleaned.length() - 100) : cleaned;
    }

    static boolean isValidImageMagicBytes(byte[] header, String declaredContentType) {
        if (header == null || header.length < 4) {
            return false;
        }
        return switch (declaredContentType) {
            case "image/jpeg", "image/jpg" ->
                    (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "image/png" -> header.length >= 8
                    && (header[0] & 0xFF) == 0x89 && header[1] == 'P' && header[2] == 'N' && header[3] == 'G'
                    && header[4] == 0x0D && header[5] == 0x0A && header[6] == 0x1A && header[7] == 0x0A;
            case "image/gif" -> header.length >= 6
                    && header[0] == 'G' && header[1] == 'I' && header[2] == 'F' && header[3] == '8'
                    && (header[4] == '7' || header[4] == '9') && header[5] == 'a';
            case "image/webp" -> header.length >= 12
                    && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private record ValidatedFile(String fileName, String contentType, long fileSize) {
    }
}
