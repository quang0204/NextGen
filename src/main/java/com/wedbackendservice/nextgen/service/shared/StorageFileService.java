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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.*;

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
    }
}
