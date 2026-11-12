package com.wedbackendservice.nextgen.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StorageCleanupScheduler {

    private final StorageFileService storageFileService;

    @Scheduled(fixedDelayString = "${minio.cleanup-interval-ms:300000}")
    public void cleanupExpiredPendingUploads() {
        try {
            int cleaned = storageFileService.cleanupExpiredPendingUploads();
            if (cleaned > 0) {
                log.info("Cleaned up {} expired pending MinIO upload sessions", cleaned);
            }
        } catch (Exception e) {
            log.debug("Skipped pending MinIO upload cleanup: {}", e.getMessage());
        }
    }
}
