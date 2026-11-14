package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.Brand;
import com.wedbackendservice.nextgen.entity.Product;
import com.wedbackendservice.nextgen.entity.ProductImage;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.BrandRepository;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
public class StorageFileService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif",
            "image/svg+xml"
    );

    /** Mỗi đánh giá tối đa 5 ảnh (API 12.2). */
    public static final int MAX_REVIEW_IMAGES = 5;

    /** Khóa Redis đánh dấu ảnh đánh giá đang được gửi kèm một đánh giá (chống dùng 1 ảnh cho 2 đánh giá). */
    public static final String REVIEW_CLAIM_PREFIX = "storage:review-claim:";

    /** Ảnh đánh giá không nhận SVG (có thể chứa script). */
    private static final Set<String> REVIEW_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final Map<String, String> MIME_TO_EXT = Map.of(
            "image/jpeg", "jpg",
            "image/jpg", "jpg",
            "image/png", "png",
            "image/webp", "webp",
            "image/gif", "gif",
            "image/svg+xml", "svg"
    );

    private final MinioStorageService minioStorageService;
    private final TokenRedisService tokenRedisService;
    private final MinioProperties minioProperties;
    private final SecurityHelper securityHelper;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final BrandRepository brandRepository;

    @Autowired
    public StorageFileService(
            MinioStorageService minioStorageService,
            TokenRedisService tokenRedisService,
            @Autowired(required = false) MinioProperties minioProperties,
            SecurityHelper securityHelper,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            @Autowired(required = false) BrandRepository brandRepository) {
        this.minioStorageService = minioStorageService;
        this.tokenRedisService = tokenRedisService;
        this.minioProperties = (minioProperties != null) ? minioProperties : new MinioProperties();
        this.securityHelper = securityHelper;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.brandRepository = brandRepository;
    }

    public StorageFileService(
            MinioStorageService minioStorageService,
            TokenRedisService tokenRedisService,
            MinioProperties minioProperties,
            SecurityHelper securityHelper,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository) {
        this(minioStorageService, tokenRedisService, minioProperties, securityHelper, productRepository, productImageRepository, null);
    }

    public List<PresignedUploadItemResponse> createPresignedUploads(PresignedUploadRequest request) {
        return generatePresignedUrls(request);
    }

    public List<PresignedUploadItemResponse> generatePresignedUrls(PresignedUploadRequest request) {
        String rawType = request.getStorageType();
        boolean isReview = rawType != null
                && (rawType.trim().equalsIgnoreCase("REVIEW") || rawType.trim().equalsIgnoreCase("REVIEWS"));

        // Ảnh đánh giá: khách đã đăng nhập tự tải lên. Các loại còn lại: chỉ STAFF / ADMIN.
        User currentUser = isReview ? securityHelper.getCurrentUser() : securityHelper.authenticateStaffOrAdmin();

        List<PresignedUploadRequest.FileItem> files = request.getFiles();
        if (files == null || files.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh sách tệp tải lên không được để trống.");
        }

        if (isReview) {
            return generateReviewPresignedUrls(currentUser, files);
        }

        if (request.getCategoryId() != null || (rawType != null && (rawType.equalsIgnoreCase("CATEGORY") || rawType.equalsIgnoreCase("CATEGORIES")))) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Danh mục sản phẩm không còn sử dụng hình ảnh/icon.");
        }
        boolean isBrand = (request.getBrandId() != null)
                || (rawType != null && (rawType.equalsIgnoreCase("BRAND") || rawType.equalsIgnoreCase("BRANDS")));

        String storageTypeStr;
        String bucket;

        if (isBrand) {
            storageTypeStr = StorageType.BRANDS.name();
            bucket = minioProperties.getBrandsBucket();
            if (files.size() > 1) {
                throw new AppException(ErrorCode.STORAGE_LIMIT_EXCEEDED,
                        "Mỗi lần chỉ được tải lên tối đa 1 logo thương hiệu");
            }
            if (request.getBrandId() != null && brandRepository != null) {
                brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
            }
        } else {
            storageTypeStr = StorageType.PRODUCTS.name();
            bucket = minioProperties.getBucket().getProducts();
            if (request.getProductId() != null && !productRepository.existsById(request.getProductId())) {
                throw new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Không tìm thấy sản phẩm có ID " + request.getProductId() + "!");
            }
            int maxFiles = minioProperties.getMaxFilesPerProduct();
            if (files.size() > maxFiles) {
                throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED,
                        "Tối đa chỉ được tải lên " + maxFiles + " ảnh mỗi lần.");
            }
        }

        int expirySeconds = Math.max(60, minioProperties.getPresignedUploadExpiryMinutes() * 60);
        long now = System.currentTimeMillis();
        long expiresAt = now + expirySeconds * 1000L;

        List<PresignedUploadItemResponse> responses = new ArrayList<>(files.size());
        for (PresignedUploadRequest.FileItem file : files) {
            String contentType = normalizeMimeType(file.getContentType());
            if (!ALLOWED_MIME_TYPES.contains(contentType)) {
                throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                        "Định dạng ảnh không hợp lệ (" + file.getContentType() + "). Chỉ chấp nhận JPEG, PNG, WEBP, GIF, SVG.");
            }

            long maxBytes = minioProperties.getMaxFileSizeBytes();
            if (file.getFileSize() == null || file.getFileSize() <= 0 || file.getFileSize() > maxBytes) {
                throw new AppException(ErrorCode.STORAGE_FILE_TOO_LARGE,
                        "Kích thước tệp '" + file.getFileName() + "' vượt quá giới hạn " + (maxBytes / (1024 * 1024)) + "MB.");
            }

            String uploadId = UUID.randomUUID().toString();
            String safeFileName = sanitizeFileName(file.getFileName(), contentType);
            String folder;
            if (isBrand) {
                folder = (request.getBrandId() != null)
                        ? "brands/" + request.getBrandId()
                        : "brands";
            } else {
                folder = (request.getProductId() != null)
                        ? String.valueOf(request.getProductId())
                        : "temp/" + currentUser.getUserId();
            }
            String objectKey = folder + "/" + uploadId + "-" + safeFileName;

            String uploadUrl = minioStorageService.generatePresignedUploadUrl(
                    bucket, objectKey, contentType, expirySeconds);

            UploadSessionData sessionData = UploadSessionData.builder()
                    .uploadId(uploadId)
                    .ownerId(currentUser.getUserId())
                    .productId(request.getProductId())
                    .brandId(request.getBrandId())
                    .categoryId(request.getCategoryId())
                    .storageType(storageTypeStr)
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .originalFileName(file.getFileName().trim())
                    .contentType(contentType)
                    .expectedSize(file.getFileSize())
                    .canonicalUrl(minioStorageService.buildCanonicalUrl(bucket, objectKey))
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
                    .expiresAt(Instant.ofEpochMilli(expiresAt))
                    .expiresAtEpochMs(expiresAt)
                    .expiresInSeconds(expirySeconds)
                    .requiredHeaders(Map.of("Content-Type", contentType))
                    .build());
        }

        return responses;
    }

    @Transactional(rollbackFor = Exception.class)
    public StorageFileResponse confirmUpload(ConfirmUploadRequest request) {
        String uploadId = request.getUploadId().trim();
        String requestedObjectKey = request.getObjectKey().trim();

        UploadSessionData session = tokenRedisService.getUploadSession(uploadId);
        if (session == null) {
            throw new AppException(ErrorCode.STORAGE_SESSION_NOT_FOUND,
                    "Phiên tải ảnh '" + uploadId + "' không tồn tại hoặc đã hết hạn.");
        }

        // Ảnh đánh giá: khách đã đăng nhập tự xác nhận (kiểm tra đúng chủ ở dưới). Loại khác: chỉ STAFF / ADMIN.
        boolean isReviewSession = StorageType.REVIEWS.name().equalsIgnoreCase(session.getStorageType());
        User currentUser = isReviewSession ? securityHelper.getCurrentUser() : securityHelper.authenticateStaffOrAdmin();

        if (!UploadSessionData.STATUS_PENDING.equals(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID,
                    "Phiên tải ảnh đã được xử lý hoặc không ở trạng thái PENDING.");
        }

        if (!Objects.equals(session.getOwnerId(), currentUser.getUserId()) && currentUser.getRole() != Role.ADMIN) {
            throw new AppException(ErrorCode.UNAUTHORIZED,
                    "Bạn không có quyền xác nhận phiên tải ảnh này.");
        }

        if (!Objects.equals(session.getObjectKey(), requestedObjectKey)) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "objectKey không khớp với phiên tải ảnh.");
        }

        if (session.getPresignedExpiresAtMillis() != null
                && session.getPresignedExpiresAtMillis() < System.currentTimeMillis()) {
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
        session.setActualSize(stat.size());
        session.setEtag(stat.etag());

        String fileUrl = minioStorageService.getFileUrl(session.getBucket(), session.getObjectKey());
        String accessUrl = minioStorageService.getAccessUrl(session.getBucket(), session.getObjectKey());
        String canonicalUrl = session.getCanonicalUrl() != null ? session.getCanonicalUrl()
                : minioStorageService.buildCanonicalUrl(session.getBucket(), session.getObjectKey());

        // Nhánh 1: Có brandId (Attach thẳng vào Brand)
        if (session.getBrandId() != null && brandRepository != null) {
            Brand brand = brandRepository.findById(session.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));

            String oldLogoUrl = brand.getLogoUrl();
            brand.setLogoUrl(canonicalUrl);
            brand.setUpdatedAt(LocalDateTime.now());
            brandRepository.save(brand);

            session.setStatus(UploadSessionData.STATUS_ATTACHED);
            runAfterCommit(() -> {
                tokenRedisService.deleteUploadSession(uploadId);
                deleteReplacedUrlObjectAfterCommit(session.getBucket(), oldLogoUrl, session.getObjectKey());
            });

            return StorageFileResponse.builder()
                    .uploadId(uploadId)
                    .brandId(session.getBrandId())
                    .bucket(session.getBucket())
                    .objectKey(session.getObjectKey())
                    .originalFileName(session.getOriginalFileName())
                    .contentType(session.getContentType())
                    .fileSize(stat.size())
                    .etag(stat.etag())
                    .canonicalUrl(canonicalUrl)
                    .accessUrl(accessUrl)
                    .imageUrl(fileUrl)
                    .status(UploadSessionData.STATUS_ATTACHED)
                    .build();
        }

        // Nhánh 2: Có productId (Lưu ProductImage)
        if (session.getProductId() != null) {
            Product product = productRepository.findById(session.getProductId())
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND,
                            "Không tìm thấy sản phẩm có ID " + session.getProductId() + "!"));

            boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());
            if (isPrimary) {
                productImageRepository.clearPrimaryByProductId(product.getProductId());
            }

            Integer resolvedOrder = request.getSortOrder();
            if (resolvedOrder == null) {
                resolvedOrder = (int) productImageRepository.countByProduct_ProductId(product.getProductId()) + 1;
            }

            ProductImage savedImage = productImageRepository.save(ProductImage.builder()
                    .product(product)
                    .imageUrl(fileUrl)
                    .bucket(session.getBucket())
                    .objectKey(session.getObjectKey())
                    .originalFileName(session.getOriginalFileName())
                    .contentType(session.getContentType())
                    .fileSize(stat.size())
                    .etag(stat.etag())
                    .isPrimary(isPrimary)
                    .displayOrder(resolvedOrder)
                    .build());

            if (isPrimary || product.getThumbnailUrl() == null || product.getThumbnailUrl().isBlank()) {
                product.setThumbnailUrl(fileUrl);
                productRepository.save(product);
            }

            session.setStatus(UploadSessionData.STATUS_COMPLETED);
            runAfterCommit(() -> tokenRedisService.deleteUploadSession(uploadId));

            return StorageFileResponse.builder()
                    .uploadId(uploadId)
                    .imageId(savedImage.getImageId())
                    .productId(session.getProductId())
                    .imageUrl(fileUrl)
                    .canonicalUrl(canonicalUrl)
                    .accessUrl(accessUrl)
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

        // Nhánh 4: Upload trước (brandId == null && categoryId == null && productId == null)
        session.setStatus(UploadSessionData.STATUS_COMPLETED);
        long ttlHours = Math.max(1, minioProperties.getCompletedSessionTtlHours());
        if (isReviewSession) {
            // Ảnh đánh giá: hết hạn mà khách chưa gửi đánh giá thì job dọn dẹp xóa ảnh khỏi MinIO.
            // Phiên giữ thêm 30 phút để job còn đọc được bucket / objectKey khi đến hạn.
            tokenRedisService.saveUploadSession(uploadId, session, Duration.ofHours(ttlHours).plusMinutes(30));
            tokenRedisService.addPendingUpload(uploadId,
                    System.currentTimeMillis() + Duration.ofHours(ttlHours).toMillis());
        } else {
            tokenRedisService.saveUploadSession(uploadId, session, Duration.ofHours(ttlHours));
        }

        boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());
        return StorageFileResponse.builder()
                .uploadId(uploadId)
                .imageUrl(fileUrl)
                .accessUrl(accessUrl)
                .canonicalUrl(canonicalUrl)
                .bucket(session.getBucket())
                .objectKey(session.getObjectKey())
                .originalFileName(session.getOriginalFileName())
                .contentType(session.getContentType())
                .fileSize(stat.size())
                .etag(stat.etag())
                .isPrimary(isPrimary)
                .sortOrder(request.getSortOrder())
                .status(UploadSessionData.STATUS_COMPLETED)
                .build();
    }

    /**
     * Ảnh đánh giá: tối đa 5 ảnh, không gắn sản phẩm / thương hiệu / danh mục.
     * Sau khi confirm, phiên được giữ trạng thái COMPLETED để API 12.2 lấy link lưu vào bảng review_images.
     */
    private List<PresignedUploadItemResponse> generateReviewPresignedUrls(
            User currentUser, List<PresignedUploadRequest.FileItem> files) {
        if (files.size() > MAX_REVIEW_IMAGES) {
            throw new AppException(ErrorCode.STORAGE_MAX_FILES_EXCEEDED,
                    "Mỗi đánh giá chỉ được tải lên tối đa " + MAX_REVIEW_IMAGES + " ảnh.");
        }

        String bucket = minioProperties.getBucket().getReviews();
        int expirySeconds = Math.max(60, minioProperties.getPresignedUploadExpiryMinutes() * 60);
        long now = System.currentTimeMillis();
        long expiresAt = now + expirySeconds * 1000L;
        long maxBytes = minioProperties.getMaxFileSizeBytes();

        List<PresignedUploadItemResponse> responses = new ArrayList<>(files.size());
        for (PresignedUploadRequest.FileItem file : files) {
            String contentType = normalizeMimeType(file.getContentType());
            if (!REVIEW_MIME_TYPES.contains(contentType)) {
                throw new AppException(ErrorCode.STORAGE_INVALID_FILE,
                        "Định dạng ảnh không hợp lệ (" + file.getContentType() + "). Ảnh đánh giá chỉ nhận JPEG, PNG, WEBP, GIF.");
            }
            if (file.getFileSize() == null || file.getFileSize() <= 0 || file.getFileSize() > maxBytes) {
                throw new AppException(ErrorCode.STORAGE_FILE_TOO_LARGE,
                        "Kích thước tệp '" + file.getFileName() + "' vượt quá giới hạn " + (maxBytes / (1024 * 1024)) + "MB.");
            }

            String uploadId = UUID.randomUUID().toString();
            String objectKey = "users/" + currentUser.getUserId() + "/" + uploadId + "-"
                    + sanitizeFileName(file.getFileName(), contentType);
            String uploadUrl = minioStorageService.generatePresignedUploadUrl(bucket, objectKey, contentType, expirySeconds);

            UploadSessionData sessionData = UploadSessionData.builder()
                    .uploadId(uploadId)
                    .ownerId(currentUser.getUserId())
                    .storageType(StorageType.REVIEWS.name())
                    .bucket(bucket)
                    .objectKey(objectKey)
                    .originalFileName(file.getFileName().trim())
                    .contentType(contentType)
                    .expectedSize(file.getFileSize())
                    .canonicalUrl(minioStorageService.buildCanonicalUrl(bucket, objectKey))
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
                    .expiresAt(Instant.ofEpochMilli(expiresAt))
                    .expiresAtEpochMs(expiresAt)
                    .expiresInSeconds(expirySeconds)
                    .requiredHeaders(Map.of("Content-Type", contentType))
                    .build());
        }
        return responses;
    }

    public String reserveInputBrandLogo(String logoUploadId, String oldLogoUrl) {
        if (logoUploadId == null || logoUploadId.isBlank()) {
            return null;
        }

        UploadSessionData session = tokenRedisService.getUploadSession(logoUploadId.trim());
        if (session == null) {
            log.info("Phiên upload {} không còn trong Redis (đã được attach hoặc đã hoàn tất)", logoUploadId);
            return null;
        }

        if (!UploadSessionData.STATUS_COMPLETED.equalsIgnoreCase(session.getStatus())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Phiên tải lên logo chưa được xác nhận hoàn tất");
        }

        if (!StorageType.BRANDS.name().equalsIgnoreCase(session.getStorageType())) {
            throw new AppException(ErrorCode.STORAGE_SESSION_INVALID, "Phiên tải lên không phải loại thương hiệu");
        }

        String bucket = minioProperties.getBrandsBucket();
        String canonicalUrl = session.getCanonicalUrl() != null
                ? session.getCanonicalUrl()
                : minioStorageService.buildCanonicalUrl(bucket, session.getObjectKey());

        runAfterCommit(() -> {
            tokenRedisService.deleteUploadSession(logoUploadId.trim());
            deleteReplacedUrlObjectAfterCommit(bucket, oldLogoUrl, session.getObjectKey());
        });

        return canonicalUrl;
    }

    public String toCanonicalUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String bucket = minioProperties.getBrandsBucket();
        String objectKey = minioStorageService.extractObjectKey(bucket, url);
        if (objectKey != null && !objectKey.isBlank()) {
            return minioStorageService.buildCanonicalUrl(bucket, objectKey);
        }
        return url.trim();
    }

    public void deleteBrandLogoObjectAfterCommit(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return;
        }
        String bucket = minioProperties.getBrandsBucket();
        String objectKey = minioStorageService.extractObjectKey(bucket, logoUrl);
        if (objectKey != null && !objectKey.isBlank()) {
            runAfterCommit(() -> minioStorageService.deleteObject(bucket, objectKey));
        }
    }

    public String resolveAccessUrl(String logoUrl) {
        if (logoUrl == null || logoUrl.isBlank()) {
            return logoUrl;
        }
        String bucket = minioProperties.getBrandsBucket();
        String objectKey = minioStorageService.extractObjectKey(bucket, logoUrl);
        if (objectKey != null && !objectKey.isBlank()) {
            return minioStorageService.getAccessUrl(bucket, objectKey);
        }
        return logoUrl;
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
                // Ảnh đánh giá đã confirm nhưng quá hạn mà không được gắn vào đánh giá nào
                boolean unusedReviewImage = StorageType.REVIEWS.name().equalsIgnoreCase(session.getStorageType())
                        && UploadSessionData.STATUS_COMPLETED.equals(session.getStatus());
                if (unusedReviewImage && tokenRedisService.getValue(REVIEW_CLAIM_PREFIX + uploadId) != null) {
                    continue; // khách đang gửi đánh giá với ảnh này, để lần dọn sau
                }
                if (UploadSessionData.STATUS_PENDING.equals(session.getStatus()) || unusedReviewImage) {
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
                    && header[1] == 'P'
                    && header[2] == 'N'
                    && header[3] == 'G'
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
            case "image/svg+xml" -> {
                String snippet = new String(header, 0, Math.min(header.length, 32)).toLowerCase();
                yield snippet.contains("<svg") || snippet.contains("<?xml");
            }
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

    private void deleteReplacedUrlObjectAfterCommit(String bucket, String oldLogoUrl, String newObjectKey) {
        if (oldLogoUrl == null || oldLogoUrl.isBlank()) {
            return;
        }
        String oldObjectKey = minioStorageService.extractObjectKey(bucket, oldLogoUrl);
        if (oldObjectKey != null && !oldObjectKey.equalsIgnoreCase(newObjectKey)) {
            runAfterCommit(() -> minioStorageService.deleteObject(bucket, oldObjectKey));
        }
    }

    private void runAfterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        action.run();
                    } catch (Exception e) {
                        log.error("Lỗi khi thực thi tác vụ sau commit", e);
                    }
                }
            });
        } else {
            action.run();
        }
    }
}
