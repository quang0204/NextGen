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
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {

    @Mock
    private MinioStorageService minioStorageService;

    @Mock
    private TokenRedisService tokenRedisService;

    @Mock
    private SecurityHelper securityHelper;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductImageRepository productImageRepository;

    private MinioProperties minioProperties;
    private StorageFileService storageFileService;
    private User adminUser;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        storageFileService = new StorageFileService(
                minioStorageService,
                tokenRedisService,
                minioProperties,
                securityHelper,
                productRepository,
                productImageRepository
        );
        adminUser = User.builder().userId(1L).role(Role.ADMIN).email("admin@nextgen.vn").build();
    }

    @Test
    @DisplayName("generatePresignedUrls creates PENDING upload sessions and adds to ZSet")
    void generatePresignedUrls_success() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);
        when(minioStorageService.generatePresignedUploadUrl(eq("products"), anyString(), eq("image/png"), anyInt()))
                .thenReturn("http://localhost:9000/products/temp/1/test.png?X-Amz-Signature=abc");

        PresignedUploadRequest request = PresignedUploadRequest.builder()
                .storageType("Product")
                .files(List.of(PresignedUploadRequest.FileItem.builder()
                        .fileName("iphone.png")
                        .contentType("image/png")
                        .fileSize(102400L)
                        .build()))
                .build();

        List<PresignedUploadItemResponse> result = storageFileService.generatePresignedUrls(request);

        assertThat(result).hasSize(1);
        PresignedUploadItemResponse item = result.get(0);
        assertThat(item.getUploadId()).isNotBlank();
        assertThat(item.getBucket()).isEqualTo("products");
        assertThat(item.getObjectKey()).startsWith("temp/1/");
        assertThat(item.getRequiredHeaders()).containsEntry("Content-Type", "image/png");

        verify(tokenRedisService).saveUploadSession(eq(item.getUploadId()), argThat(s ->
                UploadSessionData.STATUS_PENDING.equals(s.getStatus())
                        && s.getOwnerId().equals(1L)
                        && s.getObjectKey().equals(item.getObjectKey())
        ), any(Duration.class));
        verify(tokenRedisService).addPendingUpload(eq(item.getUploadId()), anyLong());
    }

    @Test
    @DisplayName("generatePresignedUrls rejects more than 10 files")
    void generatePresignedUrls_exceedsMaxFiles_throwsException() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        List<PresignedUploadRequest.FileItem> files = IntStream.range(0, 11)
                .mapToObj(i -> PresignedUploadRequest.FileItem.builder()
                        .fileName("img" + i + ".png")
                        .contentType("image/png")
                        .fileSize(1000L)
                        .build())
                .toList();

        PresignedUploadRequest request = PresignedUploadRequest.builder()
                .storageType("PRODUCT")
                .files(files)
                .build();

        assertThatThrownBy(() -> storageFileService.generatePresignedUrls(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
    }

    @Test
    @DisplayName("generatePresignedUrls rejects unsupported MIME type")
    void generatePresignedUrls_invalidMime_throwsException() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        PresignedUploadRequest request = PresignedUploadRequest.builder()
                .storageType("PRODUCT")
                .files(List.of(PresignedUploadRequest.FileItem.builder()
                        .fileName("malware.exe")
                        .contentType("application/octet-stream")
                        .fileSize(1024L)
                        .build()))
                .build();

        assertThatThrownBy(() -> storageFileService.generatePresignedUrls(request))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_INVALID_FILE);
    }

    @Test
    @DisplayName("confirmUpload without productId validates PNG magic bytes and keeps COMPLETED session for 2h")
    void confirmUpload_withoutProductId_success() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        UploadSessionData pendingSession = UploadSessionData.builder()
                .uploadId("up-123")
                .ownerId(1L)
                .productId(null)
                .storageType("PRODUCT")
                .bucket("products")
                .objectKey("temp/1/up-123-macbook.png")
                .originalFileName("macbook.png")
                .contentType("image/png")
                .expectedSize(2048L)
                .status(UploadSessionData.STATUS_PENDING)
                .presignedExpiresAtMillis(System.currentTimeMillis() + 60_000L)
                .build();

        byte[] pngHeader = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0, 0, 0, 0, 0};
        when(tokenRedisService.getUploadSession("up-123")).thenReturn(pendingSession);
        when(minioStorageService.statObject("products", "temp/1/up-123-macbook.png"))
                .thenReturn(Optional.of(new MinioStorageService.ObjectMetadata(2048L, "etag-1", "image/png")));
        when(minioStorageService.readObjectHeader("products", "temp/1/up-123-macbook.png", 16))
                .thenReturn(pngHeader);
        when(minioStorageService.getFileUrl("products", "temp/1/up-123-macbook.png"))
                .thenReturn("http://localhost:9000/products/temp/1/up-123-macbook.png");

        ConfirmUploadRequest request = ConfirmUploadRequest.builder()
                .uploadId("up-123")
                .objectKey("temp/1/up-123-macbook.png")
                .isPrimary(true)
                .build();

        StorageFileResponse response = storageFileService.confirmUpload(request);

        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getImageId()).isNull();
        assertThat(response.getImageUrl()).isEqualTo("http://localhost:9000/products/temp/1/up-123-macbook.png");

        verify(tokenRedisService).removePendingUpload("up-123");
        verify(tokenRedisService).saveUploadSession(eq("up-123"), argThat(s ->
                UploadSessionData.STATUS_COMPLETED.equals(s.getStatus()) && "etag-1".equals(s.getEtag())
        ), eq(Duration.ofHours(2)));
        verifyNoInteractions(productImageRepository);
    }

    @Test
    @DisplayName("confirmUpload with existing productId saves ProductImage, updates thumbnailUrl, and deletes Redis session")
    void confirmUpload_withProductId_savesToDbAndDeletesSession() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        Product product = Product.builder().productId(50L).productName("MacBook Pro").build();
        UploadSessionData pendingSession = UploadSessionData.builder()
                .uploadId("up-50")
                .ownerId(1L)
                .productId(50L)
                .storageType("PRODUCT")
                .bucket("products")
                .objectKey("50/up-50-mbp.jpg")
                .originalFileName("mbp.jpg")
                .contentType("image/jpeg")
                .expectedSize(4096L)
                .status(UploadSessionData.STATUS_PENDING)
                .presignedExpiresAtMillis(System.currentTimeMillis() + 60_000L)
                .build();

        byte[] jpegHeader = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        when(tokenRedisService.getUploadSession("up-50")).thenReturn(pendingSession);
        when(minioStorageService.statObject("products", "50/up-50-mbp.jpg"))
                .thenReturn(Optional.of(new MinioStorageService.ObjectMetadata(4096L, "etag-50", "image/jpeg")));
        when(minioStorageService.readObjectHeader("products", "50/up-50-mbp.jpg", 16))
                .thenReturn(jpegHeader);
        when(minioStorageService.getFileUrl("products", "50/up-50-mbp.jpg"))
                .thenReturn("http://localhost:9000/products/50/up-50-mbp.jpg");
        when(productRepository.findById(50L)).thenReturn(Optional.of(product));
        when(productImageRepository.countByProduct_ProductId(50L)).thenReturn(1L);
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setImageId(999L);
            return img;
        });

        StorageFileResponse response = storageFileService.confirmUpload(ConfirmUploadRequest.builder()
                .uploadId("up-50")
                .objectKey("50/up-50-mbp.jpg")
                .isPrimary(true)
                .build());

        assertThat(response.getImageId()).isEqualTo(999L);
        assertThat(product.getThumbnailUrl()).isEqualTo("http://localhost:9000/products/50/up-50-mbp.jpg");

        verify(productImageRepository).clearPrimaryByProductId(50L);
        ArgumentCaptor<ProductImage> imgCaptor = ArgumentCaptor.forClass(ProductImage.class);
        verify(productImageRepository).save(imgCaptor.capture());
        assertThat(imgCaptor.getValue().getObjectKey()).isEqualTo("50/up-50-mbp.jpg");
        assertThat(imgCaptor.getValue().getDisplayOrder()).isEqualTo(2);
        verify(tokenRedisService).deleteUploadSession("up-50");
    }

    @Test
    @DisplayName("confirmUpload deletes spoofed file on MinIO when magic bytes are invalid")
    void confirmUpload_invalidMagicBytes_deletesObjectAndThrows() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        UploadSessionData pendingSession = UploadSessionData.builder()
                .uploadId("up-bad")
                .ownerId(1L)
                .bucket("products")
                .objectKey("temp/1/up-bad-fake.png")
                .contentType("image/png")
                .status(UploadSessionData.STATUS_PENDING)
                .presignedExpiresAtMillis(System.currentTimeMillis() + 60_000L)
                .build();

        byte[] exeHeader = new byte[]{0x4D, 0x5A, (byte) 0x90, 0x00, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; // MZ executable
        when(tokenRedisService.getUploadSession("up-bad")).thenReturn(pendingSession);
        when(minioStorageService.statObject("products", "temp/1/up-bad-fake.png"))
                .thenReturn(Optional.of(new MinioStorageService.ObjectMetadata(512L, "etag", "image/png")));
        when(minioStorageService.readObjectHeader("products", "temp/1/up-bad-fake.png", 16))
                .thenReturn(exeHeader);

        assertThatThrownBy(() -> storageFileService.confirmUpload(ConfirmUploadRequest.builder()
                .uploadId("up-bad")
                .objectKey("temp/1/up-bad-fake.png")
                .isPrimary(false)
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_INVALID_FILE);

        verify(minioStorageService).deleteObject("products", "temp/1/up-bad-fake.png");
        verify(tokenRedisService).removePendingUpload("up-bad");
        verify(tokenRedisService).deleteUploadSession("up-bad");
    }

    @Test
    @DisplayName("confirmUpload throws STORAGE_OBJECT_NOT_UPLOADED when file missing from MinIO")
    void confirmUpload_missingObject_throwsStorageObjectNotUploaded() {
        when(securityHelper.authenticateStaffOrAdmin()).thenReturn(adminUser);

        UploadSessionData pendingSession = UploadSessionData.builder()
                .uploadId("up-missing")
                .ownerId(1L)
                .bucket("products")
                .objectKey("temp/1/up-missing.png")
                .contentType("image/png")
                .status(UploadSessionData.STATUS_PENDING)
                .presignedExpiresAtMillis(System.currentTimeMillis() + 60_000L)
                .build();

        when(tokenRedisService.getUploadSession("up-missing")).thenReturn(pendingSession);
        when(minioStorageService.statObject("products", "temp/1/up-missing.png"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storageFileService.confirmUpload(ConfirmUploadRequest.builder()
                .uploadId("up-missing")
                .objectKey("temp/1/up-missing.png")
                .isPrimary(false)
                .build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.STORAGE_OBJECT_NOT_UPLOADED);

        verify(tokenRedisService).removePendingUpload("up-missing");
        verify(tokenRedisService).deleteUploadSession("up-missing");
    }

    @Test
    @DisplayName("cleanupExpiredPendingUploads deletes orphaned MinIO objects and clears ZSet")
    void cleanupExpiredPendingUploads_cleansExpired() {
        when(tokenRedisService.getExpiredPendingUploadIds(anyLong(), eq(100L)))
                .thenReturn(Set.of("up-expired"));
        when(tokenRedisService.getUploadSession("up-expired"))
                .thenReturn(UploadSessionData.builder()
                        .uploadId("up-expired")
                        .bucket("products")
                        .objectKey("temp/1/up-expired.png")
                        .status(UploadSessionData.STATUS_PENDING)
                        .build());

        int count = storageFileService.cleanupExpiredPendingUploads();

        assertThat(count).isEqualTo(1);
        verify(minioStorageService).deleteObject("products", "temp/1/up-expired.png");
        verify(tokenRedisService).deleteUploadSession("up-expired");
        verify(tokenRedisService).removePendingUpload("up-expired");
    }
}
