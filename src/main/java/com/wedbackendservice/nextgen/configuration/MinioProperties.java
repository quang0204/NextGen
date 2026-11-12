package com.wedbackendservice.nextgen.configuration;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "minio")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MinioProperties {
    String endpoint = "http://localhost:9000";
    String publicEndpoint = "http://localhost:9000";
    String accessKey = "minioadmin";
    String secretKey = "minioadmin123";
    String region = "us-east-1";
    Bucket bucket = new Bucket();
    boolean publicRead = true;
    int presignedUploadExpiryMinutes = 15;
    int presignedGetExpiryMinutes = 60;
    int completedSessionTtlHours = 2;
    long maxFileSizeBytes = 5 * 1024 * 1024L;
    int maxFilesPerProduct = 10;

    @Getter
    @Setter
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class Bucket {
        String products = "products";
    }
}
