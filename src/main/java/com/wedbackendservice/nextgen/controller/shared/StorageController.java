package com.wedbackendservice.nextgen.controller.shared;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.shared.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.shared.storage.PresignedUploadBatchRequest;
import com.wedbackendservice.nextgen.dto.shared.storage.PresignedUploadBatchResponse;
import com.wedbackendservice.nextgen.dto.shared.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.service.shared.StorageFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class StorageController {

    private final StorageFileService storageFileService;

    /**
     * Bước 1: Cấp Presigned PUT URL (hỗ trợ nhiều ảnh) để React upload trực tiếp lên MinIO.
     */
    @PostMapping("/presigned-urls")
    public ApiResponse<PresignedUploadBatchResponse> createPresignedUrls(
            @Valid @RequestBody PresignedUploadBatchRequest request) {
        return ApiResponse.ok("Tạo presigned URL upload ảnh thành công!",
                storageFileService.createPresignedUploads(request));
    }

    /**
     * Bước 3: Xác nhận ảnh đã upload thành công trên MinIO và lưu metadata.
     */
    @PostMapping("/confirm")
    public ApiResponse<StorageFileResponse> confirmUpload(@Valid @RequestBody ConfirmUploadRequest request) {
        return ApiResponse.ok("Xác nhận upload ảnh thành công!",
                storageFileService.confirmUpload(request));
    }
}
