package com.wedbackendservice.nextgen.module.user.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginInitiateRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginVerifyOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginInitiateResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginVerifyOtpResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.mapper.UsersMapper;
import com.wedbackendservice.nextgen.module.user.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import com.wedbackendservice.nextgen.module.user.service.notification.OtpNotificationService;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceOtpTest {

  @Mock private UserRepository userRepository;
  @Mock private CartRepository cartRepository;
  @Mock private NotificationRepository notificationRepository;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private UserAddressRepository userAddressRepository;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private OtpNotificationService otpNotificationService;
  @Mock private UsersMapper usersMapper;
  @Mock private SecurityHelper securityHelper;

  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private AuthenticationService authenticationService;

  private User mockUser;
  private final String testEmail = "customer@example.com";
  private final String rawPassword = "Password@123";

  @BeforeEach
  void setUp() {
    authenticationService =
        new AuthenticationService(
            userRepository,
            cartRepository,
            notificationRepository,
            auditLogRepository,
            userAddressRepository,
            tokenRedisService,
            otpNotificationService,
            passwordEncoder,
            usersMapper,
            securityHelper);

    ReflectionTestUtils.setField(
        authenticationService,
        "signedKey",
        "1234567890123456789012345678901234567890123456789012345678901234");
    ReflectionTestUtils.setField(authenticationService, "accessExpiration", 3600L);
    ReflectionTestUtils.setField(authenticationService, "refreshExpiration", 86400L);

    mockUser =
        User.builder()
            .userId(100L)
            .email(testEmail)
            .phone("0987654321")
            .fullName("Nguyen Van A")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .role(Role.CUSTOMER)
            .status(UserStatus.ACTIVE)
            .build();
  }

  @Test
  void initiateLogin_SavesHashedOtpToRedis_AndSendsPlainOtpToUser() {
    when(tokenRedisService.isLoginBlocked(testEmail)).thenReturn(false);
    when(userRepository.findByIdentity(testEmail)).thenReturn(Optional.of(mockUser));

    LoginInitiateRequest req =
        LoginInitiateRequest.builder().identity(testEmail).password(rawPassword).build();

    LoginInitiateResponse res = authenticationService.initiateLogin(req);

    assertThat(res).isNotNull();
    assertThat(res.getSessionToken()).isNotBlank();
    assertThat(res.getChannel()).isEqualTo(OtpChannel.EMAIL);

    // 1. Kiểm tra plain OTP được gửi tới recipient qua email
    ArgumentCaptor<String> plainOtpCaptor = ArgumentCaptor.forClass(String.class);
    verify(otpNotificationService).sendOtp(eq(OtpChannel.EMAIL), eq(testEmail), plainOtpCaptor.capture());
    String sentPlainOtp = plainOtpCaptor.getValue();
    assertThat(sentPlainOtp).matches("\\d{6}");

    // 2. Kiểm tra OTP lưu lên Redis BẮT BUỘC ĐÃ MÃ HÓA (BCrypt Hash), không phải plain text
    ArgumentCaptor<OtpSessionData> sessionDataCaptor = ArgumentCaptor.forClass(OtpSessionData.class);
    verify(tokenRedisService)
        .saveOtpSession(eq(res.getSessionToken()), sessionDataCaptor.capture(), eq(120L));
    OtpSessionData savedData = sessionDataCaptor.getValue();

    assertThat(savedData.getOtpCode()).isNotEqualTo(sentPlainOtp);
    assertThat(savedData.getOtpCode()).startsWith("$2a$");
    assertThat(passwordEncoder.matches(sentPlainOtp, savedData.getOtpCode())).isTrue();
  }

  @Test
  void verifyLoginOtp_WithCorrectOtp_MatchesHashedOtpSuccessfully() {
    String sessionToken = "session-uuid-1234";
    String plainOtp = "654321";
    String hashedOtp = passwordEncoder.encode(plainOtp);

    OtpSessionData sessionData =
        OtpSessionData.builder()
            .userId(100L)
            .channel(OtpChannel.EMAIL)
            .target(testEmail)
            .otpCode(hashedOtp)
            .createdAt(LocalDateTime.now())
            .build();

    when(tokenRedisService.getOtpSession(sessionToken)).thenReturn(sessionData);
    when(userRepository.findById(100L)).thenReturn(Optional.of(mockUser));

    LoginVerifyOtpRequest req =
        LoginVerifyOtpRequest.builder().sessionToken(sessionToken).otpCode(plainOtp).build();

    LoginVerifyOtpResponse res = authenticationService.verifyLoginOtp(req);

    assertThat(res).isNotNull();
    assertThat(res.getAccessToken()).isNotBlank();
    verify(tokenRedisService).deleteOtpSession(sessionToken);
  }

  @Test
  void verifyLoginOtp_WithWrongOtp_ThrowsException() {
    String sessionToken = "session-uuid-1234";
    String plainOtp = "654321";
    String hashedOtp = passwordEncoder.encode(plainOtp);

    OtpSessionData sessionData =
        OtpSessionData.builder()
            .userId(100L)
            .channel(OtpChannel.EMAIL)
            .target(testEmail)
            .otpCode(hashedOtp)
            .createdAt(LocalDateTime.now())
            .build();

    when(tokenRedisService.getOtpSession(sessionToken)).thenReturn(sessionData);
    when(tokenRedisService.recordFailedOtpAttempt(sessionToken)).thenReturn(1L);

    LoginVerifyOtpRequest req =
        LoginVerifyOtpRequest.builder().sessionToken(sessionToken).otpCode("000000").build();

    assertThatThrownBy(() -> authenticationService.verifyLoginOtp(req))
        .isInstanceOf(AppException.class);
  }
}
