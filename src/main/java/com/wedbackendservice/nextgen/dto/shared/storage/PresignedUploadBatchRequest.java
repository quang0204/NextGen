package com.wedbackendservice.nextgen.dto.shared.storage;

import com.wedbackendservice.nextgen.enums.StorageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PresignedUploadBatchRequest {
    Long productId;
    StorageType storageType;

    @NotEmpty(message = "Danh sách tệp không được để trống")
    @Size(max = 10, message = "Tối đa 10 tệp trong một lần yêu cầu")
    @Valid
    List<FileUploadItem> files;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class FileUploadItem {
        @NotBlank(message = "fileName không được để trống")
        @Size(max = 255, message = "fileName tối đa 255 ký tự")
        String fileName;

        @NotBlank(message = "contentType không được để trống")
        String contentType;

        @NotNull(message = "fileSize không được để trống")
        @Positive(message = "fileSize phải lớn hơn 0")
        Long fileSize;
    }
}
