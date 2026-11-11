package com.wedbackendservice.nextgen.configuration;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MinioProperties.class)
public class MinioConfig {

    @Bean(name = "minioClient")
    public MinioClient minioClient(MinioProperties properties) {
        return buildClient(properties.getEndpoint(), properties);
    }

    @Bean(name = "minioPublicClient")
    public MinioClient minioPublicClient(MinioProperties properties) {
        String publicEndpoint = properties.resolvePublicEndpoint();
        if (publicEndpoint.equals(properties.getEndpoint())) {
            return minioClient(properties);
        }
        return buildClient(publicEndpoint, properties);
    }

    private MinioClient buildClient(String endpoint, MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .region(properties.getRegion())
                .credentials(properties.getAccessKey(), properties.getSecretKey())
                .build();
    }
}
