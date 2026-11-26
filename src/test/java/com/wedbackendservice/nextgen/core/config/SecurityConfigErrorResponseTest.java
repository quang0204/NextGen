package com.wedbackendservice.nextgen.core.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wedbackendservice.nextgen.core.dto.ApiResponse;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

class SecurityConfigErrorResponseTest {

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final SecurityConfig securityConfig =
      new SecurityConfig(mock(TokenRedisService.class), mock(UserRepository.class));
  private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

  @Test
  void securityEntryPointAndAccessDeniedHandler_ReturnUnifiedApiResponseJson() throws Exception {
    MockHttpServletResponse unauthRes = new MockHttpServletResponse();
    securityConfig
        .securityAuthenticationEntryPoint()
        .commence(
            new MockHttpServletRequest("GET", "/api/v1/users/profile"),
            unauthRes,
            new InsufficientAuthenticationException("Missing token"));

    assertThat(unauthRes.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
    assertThat(unauthRes.getContentType()).startsWith("application/json");
    JsonNode unauthJson = objectMapper.readTree(unauthRes.getContentAsString());
    assertThat(unauthJson.get("success").asBoolean()).isFalse();
    assertThat(unauthJson.get("statusCode").asInt()).isEqualTo(401);
    assertThat(unauthJson.get("error").asText()).isEqualTo("UNAUTHENTICATED");
    assertThat(unauthJson.get("message").asText())
        .isNotBlank()
        .doesNotContain("mật khẩu");
    assertThat(unauthJson.get("timestamp").asText()).isNotBlank();
    assertThat(unauthJson.has("data")).isFalse();

    MockHttpServletResponse forbiddenRes = new MockHttpServletResponse();
    securityConfig
        .securityAccessDeniedHandler()
        .handle(
            new MockHttpServletRequest("GET", "/api/v1/admin/users"),
            forbiddenRes,
            new AccessDeniedException("CUSTOMER forbidden"));

    assertThat(forbiddenRes.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
    assertThat(forbiddenRes.getContentType()).startsWith("application/json");
    JsonNode forbiddenJson = objectMapper.readTree(forbiddenRes.getContentAsString());
    assertThat(forbiddenJson.get("success").asBoolean()).isFalse();
    assertThat(forbiddenJson.get("statusCode").asInt()).isEqualTo(403);
    assertThat(forbiddenJson.get("error").asText()).isEqualTo("FORBIDDEN");
    assertThat(forbiddenJson.get("message").asText())
        .isEqualTo(ErrorCode.FORBIDDEN.getMessage());
    assertThat(forbiddenJson.get("timestamp").asText()).isNotBlank();
  }

  @Test
  void serviceAndControllerAccessDenied_ReturnForbiddenMatchingFilter403() {
    ResponseEntity<ApiResponse<Void>> appForbidden =
        globalExceptionHandler.handleAppException(new AppException(ErrorCode.FORBIDDEN));
    assertThat(appForbidden.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(appForbidden.getBody()).isNotNull();
    assertThat(appForbidden.getBody().getSuccess()).isFalse();
    assertThat(appForbidden.getBody().getStatusCode()).isEqualTo(403);
    assertThat(appForbidden.getBody().getError()).isEqualTo("FORBIDDEN");

    ResponseEntity<ApiResponse<Void>> springAccessDenied =
        globalExceptionHandler.handleAccessDeniedException(new AccessDeniedException("STAFF"));
    assertThat(springAccessDenied.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(springAccessDenied.getBody()).isNotNull();
    assertThat(springAccessDenied.getBody().getError()).isEqualTo("FORBIDDEN");

    ResponseEntity<ApiResponse<Void>> lockedResponse =
        globalExceptionHandler.handleAppException(
            new AppException(ErrorCode.USER_LOCKED, ErrorCode.USER_LOCKED.getMessage(), "Spam đánh giá"));
    assertThat(lockedResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    assertThat(lockedResponse.getBody()).isNotNull();
    assertThat(lockedResponse.getBody().getError()).isEqualTo("USER_LOCKED");
    assertThat(lockedResponse.getBody().getReason()).isEqualTo("Spam đánh giá");
  }
}
