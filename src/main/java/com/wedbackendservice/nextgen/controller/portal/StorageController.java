package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.service.shared.StorageFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageFileService storageFileService;

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/presigned-urls")
    public ApiResponse<List<PresignedUploadItemResponse>> generatePresignedUrls(
            @Valid @RequestBody PresignedUploadRequest request) {
        return ApiResponse.ok("Tạo presigned URL tải ảnh thành công!",
                storageFileService.generatePresignedUrls(request));
    }

    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/confirm")
    public ApiResponse<StorageFileResponse> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request) {
        return ApiResponse.ok("Xác nhận tải ảnh lên MinIO thành công!",
                storageFileService.confirmUpload(request));
    }
}
