package com.wedbackendservice.nextgen.core.exception;

import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  @ExceptionHandler(value = Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleRuntimeException(Exception exception) {
    log.error("Internal Server Error: ", exception);
    ErrorCode errorCode = ErrorCode.UNCATEGORIZE_EXCEPTION;
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(
            ApiResponse.<Void>builder()
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
    String message =
        exception.getMessage() != null && !exception.getMessage().isEmpty()
            ? exception.getMessage()
            : errorCode.getMessage();
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(errorCode.getHttpStatus().value())
                .message(message)
                .error(errorCode.name())
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(value = AccessDeniedException.class)
  public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
      AccessDeniedException exception) {
    ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
    return ResponseEntity.status(errorCode.getHttpStatus())
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(errorCode.getHttpStatus().value())
                .message(errorCode.getMessage())
                .error(errorCode.name())
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(value = MethodArgumentNotValidException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
      MethodArgumentNotValidException exception) {
    var fieldError = exception.getBindingResult().getFieldError();
    String field = fieldError != null ? fieldError.getField() : "";
    String message =
        fieldError != null ? fieldError.getDefaultMessage() : "Dữ liệu yêu cầu không hợp lệ.";
    String error = "phone".equalsIgnoreCase(field) ? "INVALID_PHONE_FORMAT" : "INVALID_REQUEST";
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(message)
                .error(error)
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(
      value = org.springframework.http.converter.HttpMessageNotReadableException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(
      org.springframework.http.converter.HttpMessageNotReadableException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Dữ liệu yêu cầu không hợp lệ.")
                .error("INVALID_REQUEST")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(
      value = org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatch(
      org.springframework.web.method.annotation.MethodArgumentTypeMismatchException exception) {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message("Giá trị tham số '" + exception.getName() + "' không hợp lệ.")
                .error("INVALID_REQUEST")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(value = org.springframework.dao.OptimisticLockingFailureException.class)
  public ResponseEntity<ApiResponse<Void>> handleOptimisticLocking(
      org.springframework.dao.OptimisticLockingFailureException exception) {
    log.warn("Optimistic locking conflict: {}", exception.getMessage());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.CONFLICT.value())
                .message(
                    "Đơn hàng đang được thao tác bởi tiến trình khác. Vui lòng tải lại trang và thử lại!")
                .error("ERR_ORDER_CONFLICT")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(value = org.springframework.web.HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleHttpMediaTypeNotSupported(
      org.springframework.web.HttpMediaTypeNotSupportedException exception) {
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value())
                .message("Định dạng dữ liệu không được hỗ trợ. Vui lòng thêm Header Content-Type: application/json.")
                .error("UNSUPPORTED_MEDIA_TYPE")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(value = org.springframework.dao.DataIntegrityViolationException.class)
  public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(
      org.springframework.dao.DataIntegrityViolationException exception) {
    log.error("Data integrity violation: ", exception);
    String rootMsg =
        exception.getRootCause() != null
            ? exception.getRootCause().getMessage()
            : exception.getMessage();
    if (rootMsg != null
        && (rootMsg.contains("uk_users_email")
            || rootMsg.contains("uk_users_phone")
            || rootMsg.toLowerCase().contains("duplicate entry"))) {
      ErrorCode err = ErrorCode.USER_ALREADY_EXISTS;
      return ResponseEntity.status(err.getHttpStatus())
          .body(
              ApiResponse.<Void>builder()
                  .success(false)
                  .statusCode(err.getHttpStatus().value())
                  .message(err.getMessage())
                  .error(err.name())
                  .timestamp(Instant.now().toString())
                  .build());
    }
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .message(
                    "Ràng buộc toàn vẹn dữ liệu: Không thể xóa hoặc thay đổi do đang có dữ liệu liên quan (sản phẩm hoặc danh mục con) trên hệ thống.")
                .error("DATA_INTEGRITY_VIOLATION")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<ApiResponse<Void>> handleAuth(AuthenticationException e) {
    ErrorCode c = ErrorCode.UNAUTHENTICATED;
    return ResponseEntity.status(c.getHttpStatus())
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(c.getHttpStatus().value())
                .message(c.getMessage())
                .error(c.name())
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoResourceFound(
      org.springframework.web.servlet.resource.NoResourceFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message("Đường dẫn không tồn tại: " + exception.getResourcePath())
                .error("NOT_FOUND")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(org.springframework.web.servlet.NoHandlerFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNoHandlerFound(
      org.springframework.web.servlet.NoHandlerFoundException exception) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.NOT_FOUND.value())
                .message("Đường dẫn không tồn tại: " + exception.getRequestURL())
                .error("NOT_FOUND")
                .timestamp(Instant.now().toString())
                .build());
  }

  @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
      org.springframework.web.HttpRequestMethodNotSupportedException exception) {
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(
            ApiResponse.<Void>builder()
                .success(false)
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value())
                .message("Phương thức HTTP không được hỗ trợ: " + exception.getMethod())
                .error("METHOD_NOT_ALLOWED")
                .timestamp(Instant.now().toString())
                .build());
  }
}
