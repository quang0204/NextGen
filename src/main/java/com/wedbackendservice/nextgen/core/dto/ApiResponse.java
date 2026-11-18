package com.wedbackendservice.nextgen.core.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
  Boolean success;
  Integer statusCode;
  String message;
  String error;
  T data;
  String timestamp;

  public static <T> ApiResponse<T> ok(T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .statusCode(200)
        .data(data)
        .timestamp(Instant.now().toString())
        .build();
  }

  public static <T> ApiResponse<T> ok(String message, T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .statusCode(200)
        .message(message)
        .data(data)
        .timestamp(Instant.now().toString())
        .build();
  }

  public static <T> ApiResponse<T> created(String message, T data) {
    return ApiResponse.<T>builder()
        .success(true)
        .statusCode(201)
        .message(message)
        .data(data)
        .timestamp(Instant.now().toString())
        .build();
  }
}
