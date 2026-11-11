package com.wedbackendservice.nextgen.configuration;

import com.wedbackendservice.nextgen.enums.StorageType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint = "http://localhost:9000";
    private String publicEndpoint;
    private String region = "us-east-1";
    private String accessKey = "";
    private String secretKey = "";
    private long presignPutExpirationSeconds = 300;
    private long presignGetExpirationSeconds = 3600;
    private boolean presignGetEnabled = true;
    private String publicBaseUrl;
    private long maxFileSizeBytes = 10_485_760L;
    private int maxFilesPerProduct = 10;
    private long cleanupIntervalMs = 300_000L;
    private long orphanMaxAgeMinutes = 180L;
    private Buckets buckets = new Buckets();

    public String resolvePublicEndpoint() {
        return (publicEndpoint != null && !publicEndpoint.isBlank()) ? publicEndpoint : endpoint;
    }

    public String resolveBucket(StorageType storageType) {
        StorageType type = storageType != null ? storageType : StorageType.PRODUCTS;
        return switch (type) {
            case PRODUCTS -> buckets.getProducts();
            case BANNERS -> buckets.getBanners();
            case AVATARS -> buckets.getAvatars();
        };
    }

    @Getter
    @Setter
    public static class Buckets {
        private String products = "products";
        private String banners = "banners";
        private String avatars = "avatars";
    }
}
