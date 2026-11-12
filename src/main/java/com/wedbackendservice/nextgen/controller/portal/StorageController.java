package com.wedbackendservice.nextgen.controller.portal;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.portal.request.storage.ConfirmUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.request.storage.PresignedUploadRequest;
import com.wedbackendservice.nextgen.dto.portal.response.storage.PresignedUploadItemResponse;
import com.wedbackendservice.nextgen.dto.portal.response.storage.StorageFileResponse;
import com.wedbackendservice.nextgen.service.shared.StorageFileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/storage")
@RequiredArgsConstructor
public class StorageController {

    private final StorageFileService storageFileService;

    /**
     * Bước 1: Tạo liên kết Presigned URL để tải trực tiếp lên MinIO (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/presigned-urls")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<List<PresignedUploadItemResponse>> createPresignedUploads(
            @Valid @RequestBody PresignedUploadRequest request) {
        List<PresignedUploadItemResponse> items = storageFileService.createPresignedUploads(request);
        return ApiResponse.created("Tạo liên kết tải lên thành công!", items);
    }

    /**
     * Bước 3: Xác nhận tải lên và kiểm tra tính toàn vẹn của tập tin trên MinIO (STAFF / ADMIN)
     */
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    @PostMapping("/confirm")
    public ApiResponse<StorageFileResponse> confirmUpload(
            @Valid @RequestBody ConfirmUploadRequest request) {
        StorageFileResponse response = storageFileService.confirmUpload(request);
        return ApiResponse.ok("Xác nhận tải lên tập tin thành công!", response);
    }
}
