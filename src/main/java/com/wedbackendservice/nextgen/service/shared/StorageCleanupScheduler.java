package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.configuration.MinioProperties;
import com.wedbackendservice.nextgen.enums.StorageType;
import com.wedbackendservice.nextgen.repository.ProductImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupScheduler {

    private final StorageService storageService;
    private final TokenRedisService tokenRedisService;
    private final ProductImageRepository productImageRepository;
    private final MinioProperties minioProperties;

    @Scheduled(fixedDelayString = "${minio.cleanup-interval-ms:300000}")
    public void cleanupExpiredAndOrphanFiles() {
        cleanupExpiredPendingSessions();
        retryOrphanDeletions();
        cleanupDanglingProductObjects();
    }

    void cleanupExpiredPendingSessions() {
        long cutoff = System.currentTimeMillis() - 60_000L;
        List<String> expiredEntries = tokenRedisService.getExpiredPendingUploadEntries(cutoff);
        for (String entry : expiredEntries) {
            String[] parts = entry.split("\\|", 3);
            if (parts.length == 3) {
                String uploadId = parts[0];
                String bucket = parts[1];
                String objectKey = parts[2];
                try {
                    if (storageService.exists(bucket, objectKey)) {
                        storageService.deleteObject(bucket, objectKey);
                    }
                    tokenRedisService.deleteUploadSession(uploadId);
                    tokenRedisService.removePendingEntry(entry);
                } catch (Exception e) {
                    log.warn("Không thể dọn pending upload hết hạn {}: {}", entry, e.getMessage());
                }
            } else {
                tokenRedisService.removePendingEntry(entry);
            }
        }
    }

    void retryOrphanDeletions() {
        for (String entry : tokenRedisService.getOrphanObjects()) {
            String[] parts = entry.split("\\|", 2);
            if (parts.length == 2) {
                try {
                    storageService.deleteObject(parts[0], parts[1]);
                    tokenRedisService.removeOrphanObject(entry);
                } catch (Exception e) {
                    log.warn("Retry xóa file mồ côi thất bại {}: {}", entry, e.getMessage());
                }
            } else {
                tokenRedisService.removeOrphanObject(entry);
            }
        }
    }

    void cleanupDanglingProductObjects() {
        try {
            String productsBucket = storageService.resolveBucket(StorageType.PRODUCTS);
            Set<String> dbKeys = productImageRepository.findAllObjectKeysByBucket(productsBucket);
            long ageCutoff = System.currentTimeMillis() - (minioProperties.getOrphanMaxAgeMinutes() * 60_000L);
            for (StorageService.StoredObjectSummary obj : storageService.listObjects(productsBucket, "")) {
                if (obj.lastModifiedEpochMs() > 0 && obj.lastModifiedEpochMs() < ageCutoff
                        && !dbKeys.contains(obj.objectKey())) {
                    storageService.deleteObject(productsBucket, obj.objectKey());
                    log.info("Cleaned dangling MinIO object: bucket={}, key={}", productsBucket, obj.objectKey());
                }
            }
        } catch (Exception e) {
            log.warn("Quét dọn file rác trong bucket products gặp lỗi: {}", e.getMessage());
        }
    }
}
