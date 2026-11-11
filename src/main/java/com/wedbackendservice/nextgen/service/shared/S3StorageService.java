package com.wedbackendservice.nextgen.service.shared;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3StorageService {
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;


    @Value("${aws.s3.bucket}")
    private String bucket;

    public void upload(String key, Path file, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromFile(file));
        log.info("Uploaded to s3: bucket={}, key={}", bucket, key);
    }
    public void download(String key, Path target) {
        s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build(),
                target);
        log.info("Downloaded from s3: bucket={}, key={} -> {}", bucket, key, target);
    }

    public void uploadBytes(String key, byte[] content, String contentType) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromBytes(content));
        log.info("Uploaded bytes to s3: bucket={}, key={}, size={}", bucket, key, content.length);
    }
    public byte[] downloadAsBytes(String key) {
        ResponseInputStream<GetObjectResponse> is = s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
        try {
            return is.readAllBytes();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Không đọc được object từ S3: key=" + key, e);
        }
    }

    public URL presignedGetUrl(String key, Duration expireAfter) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expireAfter)
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presigned = s3Presigner.presignGetObject(presignRequest);
        return presigned.url();
    }
    public boolean exits(String key){
        try{
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build())  ;
            return true;
        }catch(S3Exception e){
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                log.warn("HeadObject trả về {} cho key={}, coi như không tồn tại", e.statusCode(), key);
                return false;
            }
            throw e;

        } catch (Exception ex) {
            log.error("Exx: {}", ex.getMessage());
            return false;
        }
    }

}
