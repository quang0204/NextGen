package com.wedbackendservice.nextgen.configuration;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties minioProperties;

    @Bean(name = "minioInternalClient")
    public MinioClient minioInternalClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .region(minioProperties.getRegion())
                .build();
    }

    @Bean(name = "minioPublicClient")
    public MinioClient minioPublicClient() {
        String publicEndpoint = (minioProperties.getPublicEndpoint() != null
                && !minioProperties.getPublicEndpoint().isBlank())
                ? minioProperties.getPublicEndpoint()
                : minioProperties.getEndpoint();
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .region(minioProperties.getRegion())
                .build();
    }
}
