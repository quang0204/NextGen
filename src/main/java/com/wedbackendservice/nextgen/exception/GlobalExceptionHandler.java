package com.wedbackendservice.nextgen.exception;

import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(Exception exception) {
        log.error("Internal Server Error: ", exception);
        ErrorCode errorCode = ErrorCode.UNCATEGORIZE_EXCEPTION;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(errorCode.getHttpStatus().value())
                        .message(errorCode.getMessage())
                        .error(errorCode.name())
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse<Void>> handleAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        String message = exception.getMessage() != null && !exception.getMessage().isEmpty()
                ? exception.getMessage()
                : errorCode.getMessage();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(errorCode.getHttpStatus().value())
                        .message(message)
                        .error(errorCode.name())
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(errorCode.getHttpStatus().value())
                        .message(errorCode.getMessage())
                        .error(errorCode.name())
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        var fieldError = exception.getBindingResult().getFieldError();
        String field = fieldError != null ? fieldError.getField() : "";
        String message = fieldError != null ? fieldError.getDefaultMessage() : "Dữ liệu yêu cầu không hợp lệ.";
        String error = "phone".equalsIgnoreCase(field) ? "INVALID_PHONE_FORMAT" : "INVALID_REQUEST";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message(message)
                        .error(error)
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(value = org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Dữ liệu yêu cầu không hợp lệ.")
                        .error("INVALID_REQUEST")
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException exception) {
        log.error("Data integrity violation: ", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.<Void>builder()
                        .success(false)
                        .statusCode(HttpStatus.BAD_REQUEST.value())
                        .message("Ràng buộc toàn vẹn dữ liệu: Không thể xóa hoặc thay đổi do đang có dữ liệu liên quan (sản phẩm hoặc danh mục con) trên hệ thống.")
                        .error("DATA_INTEGRITY_VIOLATION")
                        .timestamp(Instant.now().toString())
                        .build());
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException e) {
        ErrorCode c = ErrorCode.UNAUTHENTICATED;
        return ResponseEntity.status(c.getHttpStatus()).body(
                ApiResponse.<Void>builder().success(false).statusCode(c.getHttpStatus().value())
                        .message(c.getMessage()).error(c.name()).timestamp(Instant.now().toString()).build());
    }
}
