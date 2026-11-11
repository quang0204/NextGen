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
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.enums.UploadStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import com.wedbackendservice.nextgen.repository.ProductRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageFileServiceTest {

    @Mock private StorageService storageService;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;
    @Mock private SecurityHelper securityHelper;

    private MinioProperties minioProperties;
    private StorageFileService storageFileService;

    private User adminUser;
    private User customerUser;
    private Product activeProduct;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        minioProperties.setPresignPutExpirationSeconds(300L);
        minioProperties.setMaxFileSizeBytes(10_485_760L);
        minioProperties.setMaxFilesPerProduct(10);

        storageFileService = new StorageFileService(
                storageService, tokenRedisService, productRepository,
                productImageRepository, securityHelper, minioProperties);

        adminUser = User.builder().userId(1L).role(Role.ADMIN).build();
        customerUser = User.builder().userId(2L).role(Role.CUSTOMER).build();
        activeProduct = Product.builder().productId(50L).productName("iPhone 16").status("ACTIVE").build();
    }

    @Test
    void createPresignedUploads_batchSuccess_tracksPendingSessions() {
        when(securityHelper.getCurrentUser()).thenReturn(adminUser);
        when(productRepository.findById(50L)).thenReturn(Optional.of(activeProduct));
        when(productImageRepository.countByProduct_ProductId(50L)).thenReturn(1L);
        when(storageService.generatePresignedUploadUrl(eq(StorageType.PRODUCTS), anyString(), anyString(), eq(Duration.ofSeconds(300))))
                .thenAnswer(inv -> new StorageService.PresignedUploadInfo(
                        "http://localhost:9000/products/" + inv.getArgument(1) + "?sig=1",
                        "products",
                        inv.getArgument(1),
                        StorageProvider.MINIO,
                        300L));

        PresignedUploadBatchResponse res = storageFileService.createPresignedUploads(
                PresignedUploadBatchRequest.builder()
                        .productId(50L)
                        .storageType(StorageType.PRODUCTS)
                        .files(List.of(
                                PresignedUploadBatchRequest.FileUploadItem.builder()
                                        .fileName("front.png").contentType("IMAGE/PNG").fileSize(1024L).build(),
                                PresignedUploadBatchRequest.FileUploadItem.builder()
                                        .fileName("back.jpg").contentType("image/jpeg").fileSize(2048L).build()))
                        .build());

        assertThat(res.getItems()).hasSize(2);
        assertThat(res.getItems().get(0).getHttpMethod()).isEqualTo("PUT");
        assertThat(res.getItems().get(0).getBucketName()).isEqualTo("products");
        verify(tokenRedisService, times(2)).saveUploadSession(anyString(), any(UploadSessionData.class), eq(Duration.ofSeconds(360)));
        verify(tokenRedisService, times(2)).trackPendingUpload(anyString(), eq("products"), anyString(), anyLong());
    }

    @Test
    void createPresignedUploads_rejectsCustomerForProductsAndTooManyFiles() {
        when(securityHelper.getCurrentUser()).thenReturn(customerUser);
        assertThatThrownBy(() -> storageFileService.createPresignedUploads(
                PresignedUploadBatchRequest.builder()
                        .storageType(StorageType.PRODUCTS)
                        .files(List.of(PresignedUploadBatchRequest.FileUploadItem.builder()
                                .fileName("a.png").contentType("image/png").fileSize(100L).build()))
                        .build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.UNAUTHORIZED);

        when(securityHelper.getCurrentUser()).thenReturn(adminUser);
        when(productRepository.findById(50L)).thenReturn(Optional.of(activeProduct));
        when(productImageRepository.countByProduct_ProductId(50L)).thenReturn(9L);

        assertThatThrownBy(() -> storageFileService.createPresignedUploads(
                PresignedUploadBatchRequest.builder()
                        .productId(50L)
                        .files(List.of(
                                PresignedUploadBatchRequest.FileUploadItem.builder().fileName("1.png").contentType("image/png").fileSize(100L).build(),
                                PresignedUploadBatchRequest.FileUploadItem.builder().fileName("2.png").contentType("image/png").fileSize(100L).build()))
                        .build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
    }

    @Test
    void confirmUpload_withProductId_verifiesMagicBytesAndSavesProductImage() {
        byte[] validPng = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
        UploadSessionData session = UploadSessionData.builder()
                .uploadId("u-1")
                .ownerId(1L)
                .productId(50L)
                .storageType(StorageType.PRODUCTS)
                .storageProvider(StorageProvider.MINIO)
                .bucketName("products")
                .objectKey("50/abcd-front.png")
                .originalFileName("front.png")
                .contentType("image/png")
                .expectedFileSize(2048L)
                .status(UploadStatus.PENDING)
                .expiresAtEpochMs(Instant.now().plusSeconds(300).toEpochMilli())
                .build();

        when(securityHelper.getCurrentUser()).thenReturn(adminUser);
        when(tokenRedisService.getUploadSession("u-1")).thenReturn(session);
        when(storageService.getObjectMetadata("products", "50/abcd-front.png"))
                .thenReturn(Optional.of(new StorageService.StoredObjectMeta(2048L, "image/png", "etag123")));
        when(storageService.readObjectHeader("products", "50/abcd-front.png", 16)).thenReturn(validPng);
        when(productRepository.findById(50L)).thenReturn(Optional.of(activeProduct));
        when(productImageRepository.countByProduct_ProductId(50L)).thenReturn(0L);
        when(storageService.getObjectUrl("products", "50/abcd-front.png"))
                .thenReturn("http://localhost:9000/products/50/abcd-front.png");
        when(storageService.getAccessUrl("products", "50/abcd-front.png"))
                .thenReturn("http://localhost:9000/products/50/abcd-front.png?sig=1");
        when(productImageRepository.save(any(ProductImage.class))).thenAnswer(inv -> {
            ProductImage img = inv.getArgument(0);
            img.setImageId(99L);
            return img;
        });

        StorageFileResponse res = storageFileService.confirmUpload(ConfirmUploadRequest.builder()
                .uploadId("u-1")
                .isPrimary(true)
                .build());

        assertThat(res.getStatus()).isEqualTo(UploadStatus.COMPLETED);
        assertThat(res.getImageId()).isEqualTo(99L);
        assertThat(res.getImageUrl()).contains("sig=1");
        verify(tokenRedisService).untrackPendingUpload("u-1", "products", "50/abcd-front.png");
        verify(tokenRedisService).deleteUploadSession("u-1");
    }

    @Test
    void confirmUpload_rejectsAlreadyConfirmedSpoofedKeyAndInvalidMagicBytes() {
        UploadSessionData completedSession = UploadSessionData.builder()
                .uploadId("u-done")
                .ownerId(1L)
                .status(UploadStatus.COMPLETED)
                .build();
        when(securityHelper.getCurrentUser()).thenReturn(adminUser);
        when(tokenRedisService.getUploadSession("u-done")).thenReturn(completedSession);

        assertThatThrownBy(() -> storageFileService.confirmUpload(ConfirmUploadRequest.builder().uploadId("u-done").build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STORAGE_UPLOAD_ALREADY_CONFIRMED);

        UploadSessionData pendingSession = UploadSessionData.builder()
                .uploadId("u-2")
                .ownerId(1L)
                .storageType(StorageType.PRODUCTS)
                .storageProvider(StorageProvider.MINIO)
                .bucketName("products")
                .objectKey("real.png")
                .contentType("image/png")
                .status(UploadStatus.PENDING)
                .expiresAtEpochMs(Instant.now().plusSeconds(300).toEpochMilli())
                .build();
        when(tokenRedisService.getUploadSession("u-2")).thenReturn(pendingSession);

        assertThatThrownBy(() -> storageFileService.confirmUpload(ConfirmUploadRequest.builder()
                .uploadId("u-2").objectKey("spoofed.png").build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STORAGE_OBJECT_KEY_MISMATCH);

        when(storageService.getObjectMetadata("products", "real.png"))
                .thenReturn(Optional.of(new StorageService.StoredObjectMeta(500L, "image/png", "e")));
        when(storageService.readObjectHeader("products", "real.png", 16))
                .thenReturn("<html>xss</html>".getBytes());

        assertThatThrownBy(() -> storageFileService.confirmUpload(ConfirmUploadRequest.builder().uploadId("u-2").build()))
                .isInstanceOf(AppException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.STORAGE_INVALID_FILE);

        verify(storageService).deleteObject("products", "real.png");
        verify(tokenRedisService).deleteUploadSession("u-2");
    }
}
