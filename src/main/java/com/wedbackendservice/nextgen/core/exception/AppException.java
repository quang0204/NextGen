package com.wedbackendservice.nextgen.core.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppException extends RuntimeException {
  ErrorCode errorCode;
  String reason;

  public AppException(ErrorCode errorCode) {
    this(errorCode, errorCode.getMessage(), null);
  }

  public AppException(ErrorCode errorCode, String message) {
    this(errorCode, message, null);
  }

  public AppException(ErrorCode errorCode, String message, String reason) {
    super(message);
    this.errorCode = errorCode;
    this.reason = reason;
  }
}
