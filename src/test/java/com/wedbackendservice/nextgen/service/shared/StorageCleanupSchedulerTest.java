package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageCleanupSchedulerTest {

    @Mock private StorageService storageService;
    @Mock private TokenRedisService tokenRedisService;
    @Mock private ProductImageRepository productImageRepository;

    private MinioProperties minioProperties;
    private StorageCleanupScheduler scheduler;

    @BeforeEach
    void setUp() {
        minioProperties = new MinioProperties();
        minioProperties.setOrphanMaxAgeMinutes(60L);
        scheduler = new StorageCleanupScheduler(storageService, tokenRedisService, productImageRepository, minioProperties);
    }

    @Test
    void cleanupExpiredAndOrphanFiles_removesExpiredAndOrphanObjects() {
        when(tokenRedisService.getExpiredPendingUploadEntries(anyLong()))
                .thenReturn(List.of("up-1|products|50/abandoned.png"));
        when(storageService.exists("products", "50/abandoned.png")).thenReturn(true);

        when(tokenRedisService.getOrphanObjects()).thenReturn(List.of("products|50/orphan.png"));

        when(storageService.resolveBucket(StorageType.PRODUCTS)).thenReturn("products");
        when(productImageRepository.findAllObjectKeysByBucket("products")).thenReturn(Set.of("50/kept.png"));
        long oldTimestamp = System.currentTimeMillis() - (120 * 60_000L);
        when(storageService.listObjects("products", "")).thenReturn(List.of(
                new StorageService.StoredObjectSummary("50/kept.png", oldTimestamp),
                new StorageService.StoredObjectSummary("50/dangling.png", oldTimestamp)
        ));

        scheduler.cleanupExpiredAndOrphanFiles();

        verify(storageService).deleteObject("products", "50/abandoned.png");
        verify(tokenRedisService).deleteUploadSession("up-1");
        verify(tokenRedisService).removePendingEntry("up-1|products|50/abandoned.png");

        verify(storageService).deleteObject("products", "50/orphan.png");
        verify(tokenRedisService).removeOrphanObject("products|50/orphan.png");

        verify(storageService).deleteObject("products", "50/dangling.png");
        verify(storageService, never()).deleteObject("products", "50/kept.png");
    }
}
