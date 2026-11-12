package com.wedbackendservice.nextgen.configuration;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${minio.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${minio.secret-key:minioadmin}")
    private String secretKey;

    @Value("${minio.brands-bucket:brands}")
    private String brandsBucket;

    @Bean(name = "minioClient")
    public MinioClient minioClient() {
        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        initBucket(client, brandsBucket);
        return client;
    }

    @Bean(name = "minioPublicClient")
    public MinioClient minioPublicClient() {
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    private void initBucket(MinioClient client, String bucketName) {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' created successfully", bucketName);
            }
        } catch (Exception e) {
            log.warn("Could not check/create MinIO bucket '{}' at startup (MinIO may be offline): {}", bucketName, e.getMessage());
        }
    }
}
