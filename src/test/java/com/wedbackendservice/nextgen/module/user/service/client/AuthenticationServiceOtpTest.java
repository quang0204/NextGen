package com.wedbackendservice.nextgen.module.user.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.wedbackendservice.nextgen.core.config.SecurityConfig;
import com.wedbackendservice.nextgen.core.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.core.dto.session.RegisterSessionData;
import com.wedbackendservice.nextgen.core.dto.session.RegistrationStatus;
import com.wedbackendservice.nextgen.core.dto.session.ResetPasswordSessionData;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.order.repository.OrderRepository;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ChangePasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ForgotPasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginInitiateRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginVerifyOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RefreshTokenRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RegisterVerifyOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ResetPasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.response.ForgotPasswordResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginInitiateResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginVerifyOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RefreshTokenResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RegisterVerifyOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserRegisterResponse;
import com.wedbackendservice.nextgen.module.user.dto.portal.request.UpdateUserRoleRequest;
import com.wedbackendservice.nextgen.module.user.dto.portal.request.UpdateUserStatusRequest;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.mapper.UsersMapper;
import com.wedbackendservice.nextgen.module.user.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import com.wedbackendservice.nextgen.module.user.service.notification.OtpNotificationService;
import com.wedbackendservice.nextgen.module.user.service.portal.AdminUsersService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

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

  @Test
  void verifyRegisterOtp_WhenConcurrentLockFails_ThrowsRequestInProgress() {
    String token = "reg-token-concurrent";
    String plainOtp = "123456";
    RegisterSessionData sessionData =
        RegisterSessionData.builder()
            .email(testEmail)
            .phone("0987654321")
            .fullName("Nguyen Van A")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .otpCode(passwordEncoder.encode(plainOtp))
            .status(RegistrationStatus.PENDING)
            .build();

    when(tokenRedisService.getRegisterSession(token)).thenReturn(sessionData);
    when(tokenRedisService.acquireRegisterLock(eq(token), any())).thenReturn(false);

    RegisterVerifyOtpRequest req =
        RegisterVerifyOtpRequest.builder().registrationToken(token).otpCode(plainOtp).build();

    assertThatThrownBy(() -> authenticationService.verifyRegisterOtp(req))
        .isInstanceOf(AppException.class)
        .satisfies(e -> {
          AppException appEx = (AppException) e;
          assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.REQUEST_IN_PROGRESS);
        });

    verify(userRepository, never()).save(any());
  }

  @Test
  void verifyRegisterOtp_WhenAlreadyCompleted_ReturnsIdempotentResponseWithoutCallingSave() {
    String token = "reg-token-completed";
    RegisterVerifyOtpResponse mockSavedResponse =
        RegisterVerifyOtpResponse.builder()
            .tokenType("Bearer")
            .accessToken("mock-access-token")
            .refreshToken("mock-refresh-token")
            .expiresIn(3600L)
            .build();

    RegisterSessionData sessionData =
        RegisterSessionData.builder()
            .email(testEmail)
            .phone("0987654321")
            .status(RegistrationStatus.COMPLETED)
            .completedResponse(mockSavedResponse)
            .build();

    when(tokenRedisService.getRegisterSession(token)).thenReturn(sessionData);

    RegisterVerifyOtpRequest req =
        RegisterVerifyOtpRequest.builder().registrationToken(token).otpCode("123456").build();

    RegisterVerifyOtpResponse res = authenticationService.verifyRegisterOtp(req);

    assertThat(res).isNotNull();
    assertThat(res.getAccessToken()).isEqualTo("mock-access-token");
    verify(userRepository, never()).save(any());
    verify(tokenRedisService, never()).acquireRegisterLock(anyString(), any());
  }

  @Test
  void verifyRegisterOtp_SuccessfulRegistration_SetsStateCompletedAndReleasesLock() {
    String token = "reg-token-success";
    String plainOtp = "654321";
    RegisterSessionData sessionData =
        RegisterSessionData.builder()
            .email(testEmail)
            .phone("0987654321")
            .fullName("Nguyen Van A")
            .passwordHash(passwordEncoder.encode(rawPassword))
            .otpCode(passwordEncoder.encode(plainOtp))
            .status(RegistrationStatus.PENDING)
            .build();

    when(tokenRedisService.getRegisterSession(token)).thenReturn(sessionData);
    when(tokenRedisService.acquireRegisterLock(eq(token), any())).thenReturn(true);
    when(tokenRedisService.acquireIdentityLock(eq(testEmail), any())).thenReturn(true);
    when(userRepository.countByEmailOrPhone(testEmail, "0987654321")).thenReturn(0L);
    when(userRepository.save(any(User.class))).thenReturn(mockUser);
    when(usersMapper.userRegisterResponse(any(User.class)))
        .thenReturn(UserRegisterResponse.builder().userId(100L).email(testEmail).build());

    RegisterVerifyOtpRequest req =
        RegisterVerifyOtpRequest.builder().registrationToken(token).otpCode(plainOtp).build();

    RegisterVerifyOtpResponse res = authenticationService.verifyRegisterOtp(req);

    assertThat(res).isNotNull();
    assertThat(res.getAccessToken()).isNotBlank();
    verify(userRepository, times(1)).save(any(User.class));
    verify(cartRepository, times(1)).save(any());
    verify(tokenRedisService).releaseRegisterLock(token);
    verify(tokenRedisService).releaseIdentityLock(testEmail);
  }

  @Test
  void forgotPassword_WhenRequestedAgain_InvalidatesPreviousResetToken() {
    TokenRedisService realRedisService = newInMemoryTokenRedisService();
    AuthenticationService serviceWithRealRedis =
        new AuthenticationService(
            userRepository,
            cartRepository,
            notificationRepository,
            auditLogRepository,
            userAddressRepository,
            realRedisService,
            otpNotificationService,
            passwordEncoder,
            usersMapper,
            securityHelper);

    when(userRepository.findByIdentity(testEmail)).thenReturn(Optional.of(mockUser));
    when(userRepository.findByIdentity("0987654321")).thenReturn(Optional.of(mockUser));
    when(userRepository.findById(100L)).thenReturn(Optional.of(mockUser));

    ForgotPasswordResponse firstRes =
        serviceWithRealRedis.forgotPassword(
            ForgotPasswordRequest.builder().identity(testEmail).build());
    ForgotPasswordResponse secondRes =
        serviceWithRealRedis.forgotPassword(
            ForgotPasswordRequest.builder().identity("0987654321").build());

    assertThat(secondRes.getResetToken()).isNotEqualTo(firstRes.getResetToken());

    ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
    verify(otpNotificationService, times(2)).sendOtp(any(), anyString(), otpCaptor.capture());
    String otp1 = otpCaptor.getAllValues().get(0);
    String otp2 = otpCaptor.getAllValues().get(1);

    // Token1 must be invalidated immediately once token2 is issued for the same user
    ResetPasswordRequest oldTokenReq =
        ResetPasswordRequest.builder()
            .resetToken(firstRes.getResetToken())
            .otpCode(otp1)
            .newPassword("NewPass@123")
            .confirmNewPassword("NewPass@123")
            .build();
    assertThatThrownBy(() -> serviceWithRealRedis.resetPassword(oldTokenReq))
        .isInstanceOf(AppException.class)
        .satisfies(e -> {
          ErrorCode code = ((AppException) e).getErrorCode();
          assertThat(code).isEqualTo(ErrorCode.SESSION_NOT_FOUND);
          assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        });
    verify(userRepository, never()).save(any());

    // Token2 remains valid, completes reset, and cannot be reused afterward
    ResetPasswordRequest newTokenReq =
        ResetPasswordRequest.builder()
            .resetToken(secondRes.getResetToken())
            .otpCode(otp2)
            .newPassword("NewPass@123")
            .confirmNewPassword("NewPass@123")
            .build();
    serviceWithRealRedis.resetPassword(newTokenReq);
    verify(userRepository, times(1)).save(mockUser);

    assertThatThrownBy(() -> serviceWithRealRedis.resetPassword(newTokenReq))
        .isInstanceOf(AppException.class)
        .satisfies(
            e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.SESSION_NOT_FOUND));
  }

  @Test
  void saveResetPasswordSession_PreservesSameTokenOnResendAndIsolatesDifferentUsers() {
    TokenRedisService realRedisService = newInMemoryTokenRedisService();
    ResetPasswordSessionData user1Session =
        ResetPasswordSessionData.builder().userId(100L).otpCode("otp1").build();
    ResetPasswordSessionData user2Session =
        ResetPasswordSessionData.builder().userId(200L).otpCode("otp2").build();

    realRedisService.saveResetPasswordSession("token-u1", user1Session, 300);
    realRedisService.saveResetPasswordSession(
        "token-u1", ResetPasswordSessionData.builder().userId(100L).otpCode("otp1-new").build(), 300);
    realRedisService.saveResetPasswordSession("token-u2", user2Session, 300);

    assertThat(realRedisService.getResetPasswordSession("token-u1").getOtpCode())
        .isEqualTo("otp1-new");
    assertThat(realRedisService.getResetPasswordSession("token-u2").getOtpCode()).isEqualTo("otp2");
  }

  @Test
  void resetPassword_RevokesAllDeviceAccessAndRefreshTokensAcrossRedisFlush() throws Exception {
    Map<String, String> redisStore = new ConcurrentHashMap<>();
    TokenRedisService realRedisService = newInMemoryTokenRedisService(redisStore);
    AuthenticationService service = newAuthenticationServiceWith(realRedisService);
    JwtDecoder jwtDecoder = newJwtDecoder(realRedisService);

    when(userRepository.findByIdentity(testEmail)).thenReturn(Optional.of(mockUser));
    when(userRepository.findById(100L)).thenReturn(Optional.of(mockUser));
    when(userRepository.findTokenVersionByUserId(100L))
        .thenAnswer(inv -> Optional.of(mockUser.getEffectiveTokenVersion()));

    var device1 = issueSessionTokens(service, "otp-s1");
    var device2 = issueSessionTokens(service, "otp-s2");

    assertThat(jwtDecoder.decode(device1.getAccessToken()).getSubject()).isEqualTo("100");
    assertThat(jwtDecoder.decode(device2.getAccessToken()).getSubject()).isEqualTo("100");

    ForgotPasswordResponse forgotRes =
        service.forgotPassword(ForgotPasswordRequest.builder().identity(testEmail).build());
    ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
    verify(otpNotificationService).sendOtp(any(), anyString(), otpCaptor.capture());

    service.resetPassword(
        ResetPasswordRequest.builder()
            .resetToken(forgotRes.getResetToken())
            .otpCode(otpCaptor.getValue())
            .newPassword("NewPass@123")
            .confirmNewPassword("NewPass@123")
            .build());

    assertThat(mockUser.getEffectiveTokenVersion()).isEqualTo(1L);
    assertThatThrownBy(() -> jwtDecoder.decode(device1.getAccessToken()))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtDecoder.decode(device2.getAccessToken()))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(
            () ->
                service.refreshAccessToken(
                    RefreshTokenRequest.builder().refreshToken(device1.getRefreshToken()).build()))
        .isInstanceOf(AppException.class)
        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    assertThatThrownBy(
            () ->
                service.refreshAccessToken(
                    RefreshTokenRequest.builder().refreshToken(device2.getRefreshToken()).build()))
        .isInstanceOf(AppException.class)
        .satisfies(e -> assertThat(((AppException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));

    // Even if Redis cache is wiped, DB tokenVersion still rejects all old tokens
    redisStore.clear();
    assertThatThrownBy(() -> jwtDecoder.decode(device1.getAccessToken()))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(() -> jwtDecoder.decode(device2.getAccessToken()))
        .isInstanceOf(JwtException.class);

    // New login after reset works with bumped tokenVersion = 1
    var postReset = issueSessionTokens(service, "otp-s3");
    Jwt decodedNew = jwtDecoder.decode(postReset.getAccessToken());
    assertThat(decodedNew.<Number>getClaim("ver").longValue()).isEqualTo(1L);
  }

  @Test
  void logoutAndRefreshRotation_RevokeOnlyCurrentSessionWithoutAffectingOtherDevices()
      throws Exception {
    Map<String, String> redisStore = new ConcurrentHashMap<>();
    TokenRedisService realRedisService = newInMemoryTokenRedisService(redisStore);
    AuthenticationService service = newAuthenticationServiceWith(realRedisService);
    JwtDecoder jwtDecoder = newJwtDecoder(realRedisService);

    when(userRepository.findById(100L)).thenReturn(Optional.of(mockUser));
    when(userRepository.findTokenVersionByUserId(100L))
        .thenAnswer(inv -> Optional.of(mockUser.getEffectiveTokenVersion()));

    var device1 = issueSessionTokens(service, "otp-d1");
    var device2 = issueSessionTokens(service, "otp-d2");

    // Rotate Device 1: old Device 1 tokens become invalid, Device 2 stays unaffected
    RefreshTokenResponse rotated1 =
        service.refreshAccessToken(
            RefreshTokenRequest.builder().refreshToken(device1.getRefreshToken()).build());
    assertThatThrownBy(() -> jwtDecoder.decode(device1.getAccessToken()))
        .isInstanceOf(JwtException.class);
    assertThat(jwtDecoder.decode(rotated1.getAccessToken()).getSubject()).isEqualTo("100");
    assertThat(jwtDecoder.decode(device2.getAccessToken()).getSubject()).isEqualTo("100");

    // Logout Device 1: Device 1 new tokens are revoked, Device 2 still works & refreshes
    Jwt decodedRotated1 = jwtDecoder.decode(rotated1.getAccessToken());
    when(securityHelper.getAuthentication())
        .thenReturn(new JwtAuthenticationToken(decodedRotated1));
    service.logout();

    assertThatThrownBy(() -> jwtDecoder.decode(rotated1.getAccessToken()))
        .isInstanceOf(JwtException.class);
    assertThatThrownBy(
            () ->
                service.refreshAccessToken(
                    RefreshTokenRequest.builder().refreshToken(rotated1.getRefreshToken()).build()))
        .isInstanceOf(AppException.class);

    assertThat(jwtDecoder.decode(device2.getAccessToken()).getSubject()).isEqualTo("100");
    RefreshTokenResponse refreshed2 =
        service.refreshAccessToken(
            RefreshTokenRequest.builder().refreshToken(device2.getRefreshToken()).build());
    assertThat(jwtDecoder.decode(refreshed2.getAccessToken()).getSubject()).isEqualTo("100");
  }

  @Test
  void changePasswordAndAdminActions_BumpTokenVersionAndRevokeAllDevices() throws Exception {
    Map<String, String> redisStore = new ConcurrentHashMap<>();
    TokenRedisService realRedisService = newInMemoryTokenRedisService(redisStore);
    AuthenticationService service = newAuthenticationServiceWith(realRedisService);
    JwtDecoder jwtDecoder = newJwtDecoder(realRedisService);

    when(userRepository.findById(100L)).thenReturn(Optional.of(mockUser));
    when(userRepository.findTokenVersionByUserId(100L))
        .thenAnswer(inv -> Optional.of(mockUser.getEffectiveTokenVersion()));

    // 1. Change password revokes all existing sessions
    var sessionV0 = issueSessionTokens(service, "otp-v0");
    Jwt decodedV0 = jwtDecoder.decode(sessionV0.getAccessToken());
    when(securityHelper.getAuthentication()).thenReturn(new JwtAuthenticationToken(decodedV0));
    service.changePassword(
        ChangePasswordRequest.builder()
            .currentPassword(rawPassword)
            .newPassword("Changed@123")
            .confirmNewPassword("Changed@123")
            .build());
    assertThat(mockUser.getEffectiveTokenVersion()).isEqualTo(1L);
    assertThatThrownBy(() -> jwtDecoder.decode(sessionV0.getAccessToken()))
        .isInstanceOf(JwtException.class);

    // 2. Admin updateUserRole and updateUserStatus(LOCKED) bump tokenVersion & revoke tokens
    AdminUsersService adminUsersService =
        new AdminUsersService(
            securityHelper,
            userRepository,
            userAddressRepository,
            mock(OrderRepository.class),
            auditLogRepository,
            realRedisService,
            passwordEncoder,
            usersMapper);
    User adminActor = User.builder().userId(1L).role(Role.ADMIN).status(UserStatus.ACTIVE).build();
    when(securityHelper.authenticateAdmin()).thenReturn(adminActor);

    var sessionV1 = issueSessionTokens(service, "otp-v1");
    adminUsersService.updateUserRole(
        100L, UpdateUserRoleRequest.builder().role(Role.STAFF).reason("Promote").build());
    assertThat(mockUser.getEffectiveTokenVersion()).isEqualTo(2L);
    assertThatThrownBy(() -> jwtDecoder.decode(sessionV1.getAccessToken()))
        .isInstanceOf(JwtException.class);

    var sessionV2 = issueSessionTokens(service, "otp-v2");
    UpdateUserStatusRequest lockReq = new UpdateUserStatusRequest();
    lockReq.setStatus(UserStatus.LOCKED);
    lockReq.setLockReason("Suspicious activity");
    adminUsersService.updateUserStatus(100L, lockReq);
    assertThat(mockUser.getEffectiveTokenVersion()).isEqualTo(3L);
    assertThatThrownBy(() -> jwtDecoder.decode(sessionV2.getAccessToken()))
        .isInstanceOf(JwtException.class);
  }

  private AuthenticationService newAuthenticationServiceWith(TokenRedisService redisService) {
    AuthenticationService svc =
        new AuthenticationService(
            userRepository,
            cartRepository,
            notificationRepository,
            auditLogRepository,
            userAddressRepository,
            redisService,
            otpNotificationService,
            passwordEncoder,
            usersMapper,
            securityHelper);
    ReflectionTestUtils.setField(
        svc, "signedKey", "1234567890123456789012345678901234567890123456789012345678901234");
    ReflectionTestUtils.setField(svc, "accessExpiration", 3600L);
    ReflectionTestUtils.setField(svc, "refreshExpiration", 86400L);
    return svc;
  }

  private JwtDecoder newJwtDecoder(TokenRedisService redisService) {
    SecurityConfig securityConfig = new SecurityConfig(redisService, userRepository);
    ReflectionTestUtils.setField(
        securityConfig,
        "signedKey",
        "1234567890123456789012345678901234567890123456789012345678901234");
    return securityConfig.jwtDecoder();
  }

  private LoginVerifyOtpResponse issueSessionTokens(AuthenticationService svc, String otpToken) {
    String otp = "123456";
    // Save OTP session using real TokenRedisService inside svc via Reflection or verifyLoginOtp mock?
    TokenRedisService svcRedis =
        (TokenRedisService) ReflectionTestUtils.getField(svc, "tokenRedisService");
    svcRedis.saveOtpSession(
        otpToken,
        OtpSessionData.builder()
            .userId(mockUser.getUserId())
            .channel(OtpChannel.EMAIL)
            .target(testEmail)
            .otpCode(passwordEncoder.encode(otp))
            .createdAt(LocalDateTime.now())
            .build(),
        120L);
    return svc.verifyLoginOtp(
        LoginVerifyOtpRequest.builder().sessionToken(otpToken).otpCode(otp).build());
  }

  private static TokenRedisService newInMemoryTokenRedisService() {
    return newInMemoryTokenRedisService(new ConcurrentHashMap<>());
  }

  @SuppressWarnings("unchecked")
  private static TokenRedisService newInMemoryTokenRedisService(Map<String, String> store) {
    RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class, withSettings().lenient());
    ValueOperations<String, String> valueOps = mock(ValueOperations.class, withSettings().lenient());

    when(redisTemplate.opsForValue()).thenReturn(valueOps);
    when(redisTemplate.delete(anyString()))
        .thenAnswer(inv -> store.remove(inv.getArgument(0, String.class)) != null);
    when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(Boolean.TRUE);
    when(redisTemplate.getExpire(anyString())).thenReturn(60L);
    when(valueOps.get(anyString())).thenAnswer(inv -> store.get(inv.getArgument(0, String.class)));
    doAnswer(
            inv -> {
              store.put(inv.getArgument(0, String.class), inv.getArgument(1, String.class));
              return null;
            })
        .when(valueOps)
        .set(anyString(), anyString(), any(Duration.class));
    when(valueOps.getAndSet(anyString(), anyString()))
        .thenAnswer(
            inv -> store.put(inv.getArgument(0, String.class), inv.getArgument(1, String.class)));
    when(valueOps.increment(anyString()))
        .thenAnswer(
            inv ->
                Long.valueOf(
                    store.merge(
                        inv.getArgument(0, String.class),
                        "1",
                        (oldVal, one) -> String.valueOf(Long.parseLong(oldVal) + 1L))));

    return new TokenRedisService(redisTemplate, new ObjectMapper());
  }
}
