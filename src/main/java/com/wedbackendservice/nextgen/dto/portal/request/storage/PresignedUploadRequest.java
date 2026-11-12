package com.wedbackendservice.nextgen.dto.portal.request.storage;

import com.wedbackendservice.nextgen.enums.StorageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadRequest {
    @NotNull(message = "Loại lưu trữ (storageType) không được để trống")
    StorageType storageType;

    Long brandId;

    @NotEmpty(message = "Danh sách tập tin không được để trống")
    @Valid
    List<UploadFileInput> files;
}
