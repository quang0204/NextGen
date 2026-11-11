package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.enums.StorageProvider;
import com.wedbackendservice.nextgen.enums.StorageType;
import io.minio.*;
import io.minio.errors.ErrorResponseException;
import io.minio.messages.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MinioStorageServiceTest {

    @Mock private MinioClient internalClient;
    @Mock private MinioClient signingClient;

    private MinioProperties properties;
    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        properties = new MinioProperties();
        properties.setEndpoint("http://minio:9000");
        properties.setPublicEndpoint("http://localhost:9000");
        properties.setPresignGetEnabled(true);
        properties.setPresignGetExpirationSeconds(3600L);
        minioStorageService = new MinioStorageService(internalClient, signingClient, properties);
    }

    @Test
    void generatePresignedUploadUrl_usesInternalClientForBucketAndSigningClientForUrl() throws Exception {
        when(internalClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);
        when(signingClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/products/1/abc-phone.png?X-Amz-Signature=xyz");

        StorageService.PresignedUploadInfo info = minioStorageService.generatePresignedUploadUrl(
                StorageType.PRODUCTS, "1/abc-phone.png", "image/png", Duration.ofMinutes(5));

        verify(internalClient).makeBucket(any(MakeBucketArgs.class));
        verify(signingClient).getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class));
        assertThat(info.provider()).isEqualTo(StorageProvider.MINIO);
        assertThat(info.bucketName()).isEqualTo("products");
        assertThat(info.uploadUrl()).startsWith("http://localhost:9000/products/");
    }

    @Test
    void getAccessUrl_supportsBothPresignedAndCdnPublicBaseUrl() throws Exception {
        when(signingClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("http://localhost:9000/products/a.png?X-Amz-Signature=1");

        assertThat(minioStorageService.getAccessUrl("products", "a.png")).contains("X-Amz-Signature=1");

        properties.setPublicBaseUrl("https://cdn.nextgen.vn/");
        assertThat(minioStorageService.getAccessUrl("products", "a.png"))
                .isEqualTo("https://cdn.nextgen.vn/products/a.png");
    }

    @Test
    void getObjectMetadata_returnsEmptyWhenNoSuchKey() throws Exception {
        ErrorResponseException notFound = mock(ErrorResponseException.class);
        ErrorResponse errorResponse = mock(ErrorResponse.class);
        when(errorResponse.code()).thenReturn("NoSuchKey");
        when(notFound.errorResponse()).thenReturn(errorResponse);
        when(internalClient.statObject(any(StatObjectArgs.class))).thenThrow(notFound);

        Optional<StorageService.StoredObjectMeta> meta = minioStorageService.getObjectMetadata("products", "missing.png");
        assertThat(meta).isEmpty();
    }

    @Test
    void deleteObject_delegatesToInternalMinioClient() throws Exception {
        minioStorageService.deleteObject("products", "1/old.png");
        verify(internalClient).removeObject(any(RemoveObjectArgs.class));
    }
}
