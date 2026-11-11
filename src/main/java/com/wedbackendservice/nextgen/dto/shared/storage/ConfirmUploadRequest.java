package com.wedbackendservice.nextgen.dto.shared.storage;

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

    String objectKey;
    Boolean isPrimary;
    Integer sortOrder;
}
