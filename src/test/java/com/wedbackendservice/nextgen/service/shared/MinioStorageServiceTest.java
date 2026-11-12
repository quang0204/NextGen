package com.wedbackendservice.nextgen.service.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MinioStorageServiceTest {

    private MinioStorageService minioStorageService;

    @BeforeEach
    void setUp() {
        // Khởi tạo MinioStorageService với mock hoặc null vì ta chỉ test các hàm logic offline
        minioStorageService = new MinioStorageService(null, null);
    }

    @Test
    void testBuildCanonicalUrl() {
        String canonical = minioStorageService.buildCanonicalUrl("brands", "12/apple.png");
        assertEquals("minio://brands/12/apple.png", canonical);
    }

    @Test
    void testExtractObjectKey_CanonicalUrl() {
        String key = minioStorageService.extractObjectKey("brands", "minio://brands/12/apple.png");
        assertEquals("12/apple.png", key);
    }

    @Test
    void testExtractObjectKey_DirectPrefix() {
        String key = minioStorageService.extractObjectKey("brands", "brands/12/apple.png");
        assertEquals("12/apple.png", key);
    }

    @Test
    void testExtractObjectKey_HttpPresignedUrl() {
        String key = minioStorageService.extractObjectKey("brands", "http://localhost:9000/brands/12/apple.png?X-Amz-Algorithm=AWS4-HMAC-SHA256");
        assertEquals("12/apple.png", key);
    }

    @Test
    void testExtractObjectKey_LegacyExternalUrl() {
        String key = minioStorageService.extractObjectKey("brands", "https://res.cloudinary.com/demo/image/upload/v1234/sample.jpg");
        assertNull(key, "URL ngoại legacy không được coi là đối tượng MinIO brands");
    }
}
