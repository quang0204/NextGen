package com.wedbackendservice.nextgen.dto.portal.response.storage;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageFileResponse {
    String uploadId;
    String objectKey;
    String canonicalUrl;
    String accessUrl;
    Long brandId;
    String status;
}
