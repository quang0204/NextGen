package com.wedbackendservice.nextgen.dto.portal.response.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageFileResponse {
    String uploadId;
    Long imageId;
    Long productId;
    String imageUrl;
    String bucket;
    String objectKey;
    String originalFileName;
    String contentType;
    Long fileSize;
    String etag;
    Boolean isPrimary;
    Integer sortOrder;
    String canonicalUrl;
    String accessUrl;
    Long brandId;
    Long categoryId;
    String status;
}
