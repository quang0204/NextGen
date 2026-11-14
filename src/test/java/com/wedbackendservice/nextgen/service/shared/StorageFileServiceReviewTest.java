package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.dto.session.UploadSessionData;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
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
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Ảnh đánh giá (storageType = REVIEWS) trong luồng presigned. */
@ExtendWith(MockitoExtension.class)
class StorageFileServiceReviewTest {

    @Mock private MinioStorageService minioStorageService;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private SecurityHelper securityHelper;
    @Mock private ProductRepository productRepository;
    @Mock private ProductImageRepository productImageRepository;

    private StorageFileService storageFileService;
    private User customer;

    @BeforeEach
    void setUp() {
        storageFileService = new StorageFileService(minioStorageService, tokenRedisService, new MinioProperties(),
                securityHelper, productRepository, productImageRepository);
        customer = User.builder().userId(7L).role(Role.CUSTOMER).build();
    }

    private PresignedUploadRequest reviewRequest(int count) {
        return PresignedUploadRequest.builder()
                .storageType("REVIEWS")
                .files(IntStream.range(0, count).mapToObj(i -> PresignedUploadRequest.FileItem.builder()
                        .fileName("anh" + i + ".jpg").contentType("image/jpeg").fileSize(2048L).build()).toList())
                .build();
    }

    @Test
    void customerCanRequestReviewUploadUrls() {
        when(securityHelper.getCurrentUser()).thenReturn(customer);
        when(minioStorageService.generatePresignedUploadUrl(eq("reviews"), anyString(), eq("image/jpeg"), anyInt()))
                .thenReturn("http://localhost:9000/reviews/put-url");

        List<PresignedUploadItemResponse> items = storageFileService.generatePresignedUrls(reviewRequest(2));

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getBucket()).isEqualTo("reviews");
        assertThat(items.get(0).getObjectKey()).startsWith("users/7/");
        verify(securityHelper, never()).authenticateStaffOrAdmin();
        verify(tokenRedisService, times(2)).saveUploadSession(anyString(),
                argThat(s -> "REVIEWS".equals(s.getStorageType()) && Long.valueOf(7L).equals(s.getOwnerId())),
                any(Duration.class));
    }

    @Test
    void moreThanFiveReviewImages_isRejected() {
        when(securityHelper.getCurrentUser()).thenReturn(customer);

        assertThatThrownBy(() -> storageFileService.generatePresignedUrls(reviewRequest(6)))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORAGE_MAX_FILES_EXCEEDED);
        verifyNoInteractions(minioStorageService);
    }

    @Test
    void customerStillCannotUploadProductImages() {
        when(securityHelper.authenticateStaffOrAdmin()).thenThrow(new AppException(ErrorCode.UNAUTHORIZED));

        PresignedUploadRequest request = reviewRequest(1);
        request.setStorageType("PRODUCTS");

        assertThatThrownBy(() -> storageFileService.generatePresignedUrls(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    void customerConfirmsOwnReviewUpload_keepsCompletedSession() {
        when(securityHelper.getCurrentUser()).thenReturn(customer);
        String key = "users/7/up-1-anh.jpg";
        when(tokenRedisService.getUploadSession("up-1")).thenReturn(UploadSessionData.builder()
                .uploadId("up-1").ownerId(7L).storageType("REVIEWS").bucket("reviews").objectKey(key)
                .contentType("image/jpeg").expectedSize(2048L).status(UploadSessionData.STATUS_PENDING)
                .presignedExpiresAtMillis(System.currentTimeMillis() + 60_000L).build());
        when(minioStorageService.statObject("reviews", key))
                .thenReturn(Optional.of(new MinioStorageService.ObjectMetadata(2048L, "etag", "image/jpeg")));
        when(minioStorageService.readObjectHeader("reviews", key, 16))
                .thenReturn(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});

        StorageFileResponse response = storageFileService.confirmUpload(
                ConfirmUploadRequest.builder().uploadId("up-1").objectKey(key).build());

        assertThat(response.getStatus()).isEqualTo(UploadSessionData.STATUS_COMPLETED);
        verify(securityHelper, never()).authenticateStaffOrAdmin();
        verify(tokenRedisService).saveUploadSession(eq("up-1"),
                argThat(s -> UploadSessionData.STATUS_COMPLETED.equals(s.getStatus())),
                eq(Duration.ofHours(2).plusMinutes(30)));
        // đưa lại vào hàng đợi dọn dẹp: hết 2 giờ mà chưa gửi đánh giá thì ảnh bị xóa
        verify(tokenRedisService).addPendingUpload(eq("up-1"), longThat(t ->
                Math.abs(t - (System.currentTimeMillis() + Duration.ofHours(2).toMillis())) < 60_000L));
        verifyNoInteractions(productImageRepository);
    }

    private UploadSessionData completedReview(String id) {
        return UploadSessionData.builder().uploadId(id).ownerId(7L).storageType("REVIEWS").bucket("reviews")
                .objectKey("users/7/" + id + "-anh.jpg").status(UploadSessionData.STATUS_COMPLETED).build();
    }

    @Test
    void cleanup_deletesConfirmedReviewImageThatWasNeverUsed() {
        when(tokenRedisService.getExpiredPendingUploadIds(anyLong(), anyLong())).thenReturn(java.util.Set.of("old"));
        when(tokenRedisService.getUploadSession("old")).thenReturn(completedReview("old"));

        assertThat(storageFileService.cleanupExpiredPendingUploads()).isEqualTo(1);

        verify(minioStorageService).deleteObject("reviews", "users/7/old-anh.jpg");
        verify(tokenRedisService).deleteUploadSession("old");
        verify(tokenRedisService).removePendingUpload("old");
    }

    @Test
    void cleanup_skipsReviewImageBeingSubmittedRightNow() {
        when(tokenRedisService.getExpiredPendingUploadIds(anyLong(), anyLong())).thenReturn(java.util.Set.of("busy"));
        when(tokenRedisService.getUploadSession("busy")).thenReturn(completedReview("busy"));
        when(tokenRedisService.getValue(StorageFileService.REVIEW_CLAIM_PREFIX + "busy")).thenReturn("1");

        storageFileService.cleanupExpiredPendingUploads();

        verify(minioStorageService, never()).deleteObject(anyString(), anyString());
        verify(tokenRedisService, never()).deleteUploadSession("busy");
        verify(tokenRedisService, never()).removePendingUpload("busy");
    }

    @Test
    void cleanup_keepsConfirmedProductImage() {
        when(tokenRedisService.getExpiredPendingUploadIds(anyLong(), anyLong())).thenReturn(java.util.Set.of("p1"));
        when(tokenRedisService.getUploadSession("p1")).thenReturn(UploadSessionData.builder().uploadId("p1")
                .storageType("PRODUCTS").bucket("products").objectKey("x.jpg")
                .status(UploadSessionData.STATUS_COMPLETED).build());

        storageFileService.cleanupExpiredPendingUploads();

        verify(minioStorageService, never()).deleteObject(anyString(), anyString());
    }
}
