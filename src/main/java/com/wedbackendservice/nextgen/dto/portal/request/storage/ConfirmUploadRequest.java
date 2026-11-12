package com.wedbackendservice.nextgen.dto.portal.request.storage;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ConfirmUploadRequest {

    @NotBlank(message = "uploadId không được để trống")
    String uploadId;

    @NotBlank(message = "objectKey không được để trống")
    String objectKey;

    Boolean isPrimary;

    Integer sortOrder;
}
