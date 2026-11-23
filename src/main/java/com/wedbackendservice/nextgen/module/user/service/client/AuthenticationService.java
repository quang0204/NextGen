package com.wedbackendservice.nextgen.module.user.service.client;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wedbackendservice.nextgen.core.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.core.dto.session.RegisterSessionData;
import com.wedbackendservice.nextgen.core.dto.session.RegistrationStatus;
import com.wedbackendservice.nextgen.core.dto.session.ResetPasswordSessionData;
import com.wedbackendservice.nextgen.core.dto.session.TokenPair;
import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.core.util.ClientIpHelper;
import com.wedbackendservice.nextgen.module.cart.entity.Cart;
import com.wedbackendservice.nextgen.module.cart.repository.CartRepository;
import com.wedbackendservice.nextgen.module.notification.entity.Notification;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.notification.repository.NotificationRepository;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ChangePasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ForgotPasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ForgotResendOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginInitiateRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.LoginVerifyOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RefreshTokenRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RegisterInitiateRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RegisterResendOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.RegisterVerifyOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ResendOtpRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.request.ResetPasswordRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.response.AddressResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.AuthenticationResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.ForgotPasswordResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.ForgotResendOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginInitiateResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.LoginVerifyOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RefreshTokenResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RegisterInitiateResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RegisterResendOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.RegisterVerifyOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.ResendOtpResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserMeResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.OtpChannel;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.mapper.UsersMapper;
import com.wedbackendservice.nextgen.module.user.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import com.wedbackendservice.nextgen.module.user.service.notification.OtpNotificationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthenticationService {

  private static final int OTP_EXPIRE_SECONDS = 120;
  private static final int OTP_COOLDOWN_SECONDS = 60;
  private static final int REGISTRATION_EXPIRE_SECONDS = 300;
  private static final java.time.Duration REGISTER_LOCK_TTL = java.time.Duration.ofSeconds(10);
  private static final long REGISTER_IDEMPOTENT_TTL_SECONDS = 60L;
  private static final int MAX_FAILED_LOGIN = 5;
  private static final String TOKEN_TYPE_CLAIM = "token_type";
  private static final String TYPE_ACCESS = "access";
  private static final String TYPE_REFRESH = "refresh";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  @NonFinal
  @Value("${jwt.signed-key:${jwt.signed_key}}")
  protected String signedKey;

  @NonFinal
  @Value("${jwt.access-expiration}")
  protected Long accessExpiration;

  @NonFinal
  @Value("${jwt.refresh-expiration}")
  protected Long refreshExpiration;

  private final UserRepository userRepository;
  private final CartRepository cartRepository;
  private final NotificationRepository notificationRepository;
  private final AuditLogRepository auditLogRepository;
  private final UserAddressRepository userAddressRepository;

  private final TokenRedisService tokenRedisService;
  private final OtpNotificationService otpNotificationService;
  private final PasswordEncoder passwordEncoder;
  private final UsersMapper usersMapper;
  private final SecurityHelper securityHelper;

  // =====================================================================
  // LOGIN
  // =====================================================================

  /** API 1: Đăng nhập bước 1 - kiểm tra mật khẩu, sinh và gửi OTP. */
  public LoginInitiateResponse initiateLogin(LoginInitiateRequest request) {
    String identity = request.getIdentity().trim();

    if (tokenRedisService.isLoginBlocked(identity)) {
      log.warn("Identity {} bị tạm khóa do nhập sai quá {} lần", identity, MAX_FAILED_LOGIN);
      throw new AppException(ErrorCode.TOO_MANY_REQUESTS);
    }

    User user = userRepository.findByIdentity(identity).orElseThrow(() -> failedLogin(identity));

    if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
      throw failedLogin(identity);
    }

    assertNotLocked(user);
    tokenRedisService.clearFailedLoginAttempts(identity);

    OtpChannel channel = resolveChannel(identity);
    String target = resolveTarget(channel, user, identity);

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, target);

    String otpCode = generateOtp();
    String sessionToken = UUID.randomUUID().toString().replace("-", "");

    tokenRedisService.saveOtpSession(
        sessionToken,
        OtpSessionData.builder()
            .userId(user.getUserId())
            .channel(channel)
            .target(target)
            .otpCode(passwordEncoder.encode(otpCode))
            .createdAt(LocalDateTime.now())
            .build(),
        OTP_EXPIRE_SECONDS);

    recordOtpSendAttempt(clientIp, target);
    otpNotificationService.sendOtp(channel, target, otpCode);

    return LoginInitiateResponse.builder()
        .channel(channel)
        .targetMasked(maskTarget(channel, target))
        .sessionToken(sessionToken)
        .expiresInSeconds(OTP_EXPIRE_SECONDS)
        .cooldownSeconds(OTP_COOLDOWN_SECONDS)
        .build();
  }

  /** API 2: Đăng nhập bước 2 - xác thực OTP, cấp JWT. */
  public LoginVerifyOtpResponse verifyLoginOtp(LoginVerifyOtpRequest request) {
    String sessionToken = request.getSessionToken().trim();

    OtpSessionData sessionData = tokenRedisService.getOtpSession(sessionToken);
    if (sessionData == null) {
      throw new AppException(ErrorCode.SESSION_NOT_FOUND);
    }
    if (!otpMatches(sessionData.getOtpCode(), request.getOtpCode())) {
      long failedAttempts = tokenRedisService.recordFailedOtpAttempt(sessionToken);
      if (failedAttempts >= TokenRedisService.MAX_OTP_FAIL_ATTEMPTS) {
        tokenRedisService.deleteOtpSession(sessionToken);
        tokenRedisService.clearFailedOtpAttempts(sessionToken);
        log.warn(
            "Phiên OTP đăng nhập [{}] bị hủy do nhập sai quá {} lần",
            sessionToken,
            TokenRedisService.MAX_OTP_FAIL_ATTEMPTS);
        throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
      }
      int remaining = TokenRedisService.MAX_OTP_FAIL_ATTEMPTS - (int) failedAttempts;
      throw new AppException(
          ErrorCode.OTP_INVALID_OR_EXPIRED,
          "Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
    }
    tokenRedisService.clearFailedOtpAttempts(sessionToken);
    tokenRedisService.deleteOtpSession(sessionToken);

    User user = findUser(sessionData.getUserId());
    assertNotLocked(user);

    AuthenticationResponse auth = generateToken(user);
    return LoginVerifyOtpResponse.builder()
        .user(usersMapper.usersResponse(user))
        .accessToken(auth.getAccessToken())
        .refreshToken(auth.getRefreshToken())
        .expiresIn(accessExpiration)
        .build();
  }

  /** API 3: Gửi lại OTP đăng nhập. */
  public ResendOtpResponse resendLoginOtp(ResendOtpRequest request) {
    String sessionToken = request.getSessionToken().trim();

    OtpSessionData sessionData = tokenRedisService.getOtpSession(sessionToken);
    if (sessionData == null) {
      throw new AppException(ErrorCode.SESSION_NOT_FOUND);
    }

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, sessionData.getTarget());
    assertCooldownPassed(sessionData.getCreatedAt(), sessionToken);

    String newOtp = generateOtp();
    tokenRedisService.saveOtpSession(
        sessionToken,
        OtpSessionData.builder()
            .userId(sessionData.getUserId())
            .channel(sessionData.getChannel())
            .target(sessionData.getTarget())
            .otpCode(passwordEncoder.encode(newOtp))
            .createdAt(LocalDateTime.now())
            .build(),
        OTP_EXPIRE_SECONDS);
    recordOtpSendAttempt(clientIp, sessionData.getTarget());

    otpNotificationService.sendOtp(sessionData.getChannel(), sessionData.getTarget(), newOtp);

    return ResendOtpResponse.builder()
        .cooldownSeconds(OTP_COOLDOWN_SECONDS)
        .expiresInSeconds(OTP_EXPIRE_SECONDS)
        .build();
  }

  // =====================================================================
  // REGISTER
  // =====================================================================

  /** API 4: Đăng ký bước 1. */
  public RegisterInitiateResponse initiateRegister(RegisterInitiateRequest request) {
    String email = request.getEmail().trim();
    String phone = request.getPhone().trim();

    if (!request.getPassword().equals(request.getConfirmPassword())) {
      throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
    }
    assertUserNotExists(email, phone);

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, email);

    String otpCode = generateOtp();
    String registrationToken = UUID.randomUUID().toString().replace("-", "");

    tokenRedisService.saveRegisterSession(
        registrationToken,
        RegisterSessionData.builder()
            .fullName(request.getFullName().trim())
            .phone(phone)
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .otpCode(passwordEncoder.encode(otpCode))
            .createdAt(LocalDateTime.now())
            .status(RegistrationStatus.PENDING)
            .build(),
        REGISTRATION_EXPIRE_SECONDS);

    recordOtpSendAttempt(clientIp, email);
    otpNotificationService.sendOtp(OtpChannel.EMAIL, email, otpCode);

    return RegisterInitiateResponse.builder()
        .channel(OtpChannel.EMAIL)
        .targetEmail(email)
        .registrationToken(registrationToken)
        .expiresInSeconds(REGISTRATION_EXPIRE_SECONDS)
        .cooldownSeconds(OTP_COOLDOWN_SECONDS)
        .build();
  }

  /** API 5: Đăng ký bước 2 - xác thực OTP, tạo User + Cart + Notification, cấp JWT. */
  @Transactional
  public RegisterVerifyOtpResponse verifyRegisterOtp(RegisterVerifyOtpRequest request) {
    String registrationToken = request.getRegistrationToken().trim();

    // 1. Kiểm tra session từ Redis
    RegisterSessionData sessionData = tokenRedisService.getRegisterSession(registrationToken);
    if (sessionData == null) {
      throw new AppException(
          ErrorCode.SESSION_NOT_FOUND, "registrationToken không tồn tại hoặc đã hết hạn.");
    }

    // 2. IDEMPOTENCY: Nếu phiên đăng ký đã COMPLETED thành công trước đó, trả về ngay kết quả hợp lệ
    if (sessionData.getStatus() == RegistrationStatus.COMPLETED) {
      if (sessionData.getCompletedResponse() != null) {
        log.info(
            "Idempotent hit: Phiên đăng ký [{}] đã kích hoạt thành công trước đó.",
            registrationToken);
        return sessionData.getCompletedResponse();
      }
      User existingUser = userRepository.findByIdentity(sessionData.getEmail()).orElse(null);
      if (existingUser != null) {
        AuthenticationResponse auth = generateToken(existingUser);
        return RegisterVerifyOtpResponse.builder()
            .user(usersMapper.userRegisterResponse(existingUser))
            .tokenType("Bearer")
            .accessToken(auth.getAccessToken())
            .refreshToken(auth.getRefreshToken())
            .expiresIn(accessExpiration)
            .build();
      }
    }

    // 3. TẦNG 1: REDIS ATOMIC LOCK theo registrationToken & email
    boolean tokenLocked =
        tokenRedisService.acquireRegisterLock(registrationToken, REGISTER_LOCK_TTL);
    if (!tokenLocked) {
      log.warn("Yêu cầu trùng lặp bị chặn bởi Redis Lock trên token [{}]", registrationToken);
      throw new AppException(ErrorCode.REQUEST_IN_PROGRESS);
    }

    boolean identityLocked =
        tokenRedisService.acquireIdentityLock(sessionData.getEmail(), REGISTER_LOCK_TTL);
    if (!identityLocked) {
      tokenRedisService.releaseRegisterLock(registrationToken);
      log.warn("Yêu cầu trùng lặp bị chặn bởi Redis Lock trên email [{}]", sessionData.getEmail());
      throw new AppException(ErrorCode.REQUEST_IN_PROGRESS);
    }

    try {
      // Re-read session data sau khi acquire lock (Double-checked locking)
      sessionData = tokenRedisService.getRegisterSession(registrationToken);
      if (sessionData == null) {
        throw new AppException(
            ErrorCode.SESSION_NOT_FOUND, "registrationToken không tồn tại hoặc đã hết hạn.");
      }
      if (sessionData.getStatus() == RegistrationStatus.COMPLETED) {
        if (sessionData.getCompletedResponse() != null) {
          return sessionData.getCompletedResponse();
        }
      }

      // 4. KIỂM TRA MÃ OTP
      if (!otpMatches(sessionData.getOtpCode(), request.getOtpCode())) {
        long failedAttempts = tokenRedisService.recordFailedOtpAttempt(registrationToken);
        if (failedAttempts >= TokenRedisService.MAX_OTP_FAIL_ATTEMPTS) {
          tokenRedisService.deleteRegisterSession(registrationToken);
          tokenRedisService.clearFailedOtpAttempts(registrationToken);
          log.warn(
              "Phiên đăng ký [{}] bị hủy do nhập sai OTP quá {} lần",
              registrationToken,
              TokenRedisService.MAX_OTP_FAIL_ATTEMPTS);
          throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
        }
        int remaining = TokenRedisService.MAX_OTP_FAIL_ATTEMPTS - (int) failedAttempts;
        throw new AppException(
            ErrorCode.OTP_INVALID_OR_EXPIRED,
            "Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
      }

      // 5. TẦNG 2: STATE MACHINE -> Chuyển sang PROCESSING
      sessionData.setStatus(RegistrationStatus.PROCESSING);
      tokenRedisService.saveRegisterSession(
          registrationToken, sessionData, REGISTER_LOCK_TTL.toSeconds());

      // 6. KIỂM TRA TỒN TẠI (TẦNG ỨNG DỤNG)
      assertUserNotExists(sessionData.getEmail(), sessionData.getPhone());
      tokenRedisService.clearFailedOtpAttempts(registrationToken);

      // 7. TẦNG 4: LƯU DB (CÓ DATABASE UNIQUE CONSTRAINT BẢO VỆ)
      User newUser =
          userRepository.save(
              User.builder()
                  .fullName(sessionData.getFullName())
                  .phone(sessionData.getPhone())
                  .email(sessionData.getEmail())
                  .passwordHash(sessionData.getPasswordHash())
                  .role(Role.CUSTOMER)
                  .status(UserStatus.ACTIVE)
                  .build());

      cartRepository.save(Cart.builder().user(newUser).build());
      notificationRepository.save(
          buildNotification(
              newUser,
              "Chào mừng thành viên mới",
              "Chào mừng bạn đến với NextGen! Tài khoản của bạn đã được kích hoạt thành công."));

      AuthenticationResponse auth = generateToken(newUser);
      RegisterVerifyOtpResponse response =
          RegisterVerifyOtpResponse.builder()
              .user(usersMapper.userRegisterResponse(newUser))
              .tokenType("Bearer")
              .accessToken(auth.getAccessToken())
              .refreshToken(auth.getRefreshToken())
              .expiresIn(accessExpiration)
              .build();

      // 8. TẦNG 3: STATE MACHINE -> COMPLETED & LƯU PHẢN HỒI CHO IDEMPOTENCY (giữ 60s)
      sessionData.setStatus(RegistrationStatus.COMPLETED);
      sessionData.setCreatedUserId(newUser.getUserId());
      sessionData.setCompletedResponse(response);
      tokenRedisService.saveRegisterSession(
          registrationToken, sessionData, REGISTER_IDEMPOTENT_TTL_SECONDS);

      return response;
    } finally {
      tokenRedisService.releaseRegisterLock(registrationToken);
      tokenRedisService.releaseIdentityLock(sessionData.getEmail());
    }
  }

  /** API 6: Gửi lại OTP đăng ký. */
  public RegisterResendOtpResponse resendRegisterOtp(RegisterResendOtpRequest request) {
    String registrationToken = request.getRegistrationToken().trim();

    RegisterSessionData sessionData = tokenRedisService.getRegisterSession(registrationToken);
    if (sessionData == null) {
      throw new AppException(
          ErrorCode.SESSION_NOT_FOUND, "registrationToken không tồn tại hoặc đã hết hạn.");
    }
    if (sessionData.getStatus() == RegistrationStatus.COMPLETED) {
      throw new AppException(
          ErrorCode.USER_ALREADY_EXISTS, "Tài khoản đã được đăng ký và kích hoạt thành công!");
    }

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, sessionData.getEmail());
    assertCooldownPassed(sessionData.getCreatedAt(), registrationToken);

    String newOtp = generateOtp();
    tokenRedisService.saveRegisterSession(
        registrationToken,
        RegisterSessionData.builder()
            .fullName(sessionData.getFullName())
            .phone(sessionData.getPhone())
            .email(sessionData.getEmail())
            .passwordHash(sessionData.getPasswordHash())
            .otpCode(passwordEncoder.encode(newOtp))
            .createdAt(LocalDateTime.now())
            .build(),
        REGISTRATION_EXPIRE_SECONDS);
    recordOtpSendAttempt(clientIp, sessionData.getEmail());

    otpNotificationService.sendOtp(OtpChannel.EMAIL, sessionData.getEmail(), newOtp);

    return RegisterResendOtpResponse.builder()
        .cooldownSeconds(OTP_COOLDOWN_SECONDS)
        .expiresInSeconds(REGISTRATION_EXPIRE_SECONDS)
        .build();
  }

  // =====================================================================
  // FORGOT / RESET PASSWORD
  // =====================================================================

  /** API 7: Quên mật khẩu. */
  public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest request) {
    String identity = request.getIdentity().trim();

    User user =
        userRepository
            .findByIdentity(identity)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

    OtpChannel channel = resolveChannel(identity);
    String target = resolveTarget(channel, user, identity);

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, target);

    String resetToken = UUID.randomUUID().toString().replace("-", "");
    String otpCode = generateOtp();

    tokenRedisService.saveResetPasswordSession(
        resetToken,
        ResetPasswordSessionData.builder()
            .userId(user.getUserId())
            .identity(identity)
            .channel(channel)
            .target(target)
            .otpCode(passwordEncoder.encode(otpCode))
            .createdAt(LocalDateTime.now())
            .build(),
        REGISTRATION_EXPIRE_SECONDS);

    recordOtpSendAttempt(clientIp, target);
    otpNotificationService.sendOtp(channel, target, otpCode);

    return ForgotPasswordResponse.builder()
        .resetToken(resetToken)
        .channel(channel)
        .targetMasked(maskTarget(channel, target))
        .expiresInSeconds(REGISTRATION_EXPIRE_SECONDS)
        .cooldownSeconds(OTP_COOLDOWN_SECONDS)
        .build();
  }

  /** API 8: Đặt mật khẩu mới. */
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
      throw new AppException(ErrorCode.PASSWORD_NOT_MATCH);
    }

    String resetToken = request.getResetToken().trim();
    ResetPasswordSessionData sessionData = tokenRedisService.getResetPasswordSession(resetToken);
    if (sessionData == null) {
      throw new AppException(
          ErrorCode.SESSION_NOT_FOUND, "resetToken không tồn tại hoặc đã hết hạn.");
    }
    if (!otpMatches(sessionData.getOtpCode(), request.getOtpCode())) {
      long failedAttempts = tokenRedisService.recordFailedOtpAttempt(resetToken);
      if (failedAttempts >= TokenRedisService.MAX_OTP_FAIL_ATTEMPTS) {
        tokenRedisService.deleteResetPasswordSession(resetToken);
        tokenRedisService.clearFailedOtpAttempts(resetToken);
        log.warn(
            "Phiên đặt lại mật khẩu [{}] bị hủy do nhập sai OTP quá {} lần",
            resetToken,
            TokenRedisService.MAX_OTP_FAIL_ATTEMPTS);
        throw new AppException(ErrorCode.OTP_ATTEMPTS_EXCEEDED);
      }
      int remaining = TokenRedisService.MAX_OTP_FAIL_ATTEMPTS - (int) failedAttempts;
      throw new AppException(
          ErrorCode.OTP_INVALID_OR_EXPIRED,
          "Mã OTP không chính xác. Bạn còn " + remaining + " lần thử.");
    }
    tokenRedisService.clearFailedOtpAttempts(resetToken);
    tokenRedisService.deleteResetPasswordSession(resetToken);

    User user = findUser(sessionData.getUserId());
    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    tokenRedisService.deleteToken(user.getUserId());

    auditLogRepository.log(user, "PASSWORD_RESET", "User", user.getUserId(), null, null);

    notificationRepository.save(
        buildNotification(
            user,
            "Mật khẩu đã thay đổi",
            "Mật khẩu tài khoản của bạn vừa được thay đổi thành công."));
  }

  /** API 9: Gửi lại OTP quên mật khẩu. */
  public ForgotResendOtpResponse resendForgotPasswordOtp(ForgotResendOtpRequest request) {
    String resetToken = request.getResetToken().trim();

    ResetPasswordSessionData sessionData = tokenRedisService.getResetPasswordSession(resetToken);
    if (sessionData == null) {
      throw new AppException(
          ErrorCode.SESSION_NOT_FOUND, "resetToken không tồn tại hoặc đã hết hạn.");
    }

    String clientIp = ClientIpHelper.getClientIp();
    assertOtpSendingAllowed(clientIp, sessionData.getTarget());
    assertCooldownPassed(sessionData.getCreatedAt(), resetToken);

    String newOtp = generateOtp();
    tokenRedisService.saveResetPasswordSession(
        resetToken,
        ResetPasswordSessionData.builder()
            .userId(sessionData.getUserId())
            .identity(sessionData.getIdentity())
            .channel(sessionData.getChannel())
            .target(sessionData.getTarget())
            .otpCode(passwordEncoder.encode(newOtp))
            .createdAt(LocalDateTime.now())
            .build(),
        REGISTRATION_EXPIRE_SECONDS);
    recordOtpSendAttempt(clientIp, sessionData.getTarget());

    otpNotificationService.sendOtp(sessionData.getChannel(), sessionData.getTarget(), newOtp);

    return ForgotResendOtpResponse.builder().cooldownSeconds(OTP_COOLDOWN_SECONDS).build();
  }

  // =====================================================================
  // TOKEN / SESSION
  // =====================================================================

  /** API 10: Cấp lại Access Token (Refresh Token Rotation). */
  public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest request)
      throws ParseException, JOSEException {
    SignedJWT signedJWT = verifyToken(request.getRefreshToken(), TYPE_REFRESH);
    Long userId = Long.valueOf(signedJWT.getJWTClaimsSet().getSubject());

    TokenPair tokenPair = tokenRedisService.getTokenPair(userId);
    if (tokenPair == null || !tokenPair.getRefreshToken().equals(request.getRefreshToken())) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    User user = findUser(userId);
    assertNotLocked(user);

    Date oldRefreshExp = signedJWT.getJWTClaimsSet().getExpirationTime();
    tokenRedisService.blacklistToken(userId, tokenPair, oldRefreshExp);

    AuthenticationResponse authenticationResponse = generateToken(user);
    return RefreshTokenResponse.builder()
        .accessToken(authenticationResponse.getAccessToken())
        .refreshToken(authenticationResponse.getRefreshToken())
        .expiresIn(accessExpiration)
        .build();
  }

  /** API 11: Đăng xuất. */
  public void logout() throws ParseException {
    Long userId = Long.valueOf(securityHelper.getAuthentication().getName());
    TokenPair tokenPair = tokenRedisService.getTokenPair(userId);
    if (tokenPair != null) {
      Date refreshExp =
          SignedJWT.parse(tokenPair.getRefreshToken()).getJWTClaimsSet().getExpirationTime();
      tokenRedisService.blacklistToken(userId, tokenPair, refreshExp);
    }
    tokenRedisService.deleteToken(userId);
    log.info("User {} logged out — token pair blacklisted, session removed", userId);
  }

  /** API 12: Thông tin người dùng hiện tại. */
  public UserMeResponse getCurrentUser() {
    Long userId = Long.valueOf(securityHelper.getAuthentication().getName());
    User user = findUser(userId);
    AddressResponse defaultAddress =
        userAddressRepository
            .findByUserIdAndIsDefaultTrue(userId)
            .map(usersMapper::toAddressResponse)
            .orElse(null);

    UserMeResponse response = usersMapper.toUserMeResponse(user);
    response.setDefaultAddress(defaultAddress);
    return response;
  }

  /** API 13: Đổi mật khẩu (Change Password). */
  @Transactional
  public void changePassword(ChangePasswordRequest request) {
    Long userId = Long.valueOf(securityHelper.getAuthentication().getName());
    User user = findUser(userId);
    assertNotLocked(user);

    if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
      throw new AppException(ErrorCode.PASSWORD_CONFIRM_NOT_MATCH);
    }

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
      throw new AppException(ErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    if (request.getCurrentPassword().equals(request.getNewPassword())
        || passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
      throw new AppException(ErrorCode.NEW_PASSWORD_SAME_AS_CURRENT);
    }

    user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    auditLogRepository.log(
        user, "CHANGE_PASSWORD", "User", user.getUserId(), "HASH_PROTECTED", "HASH_UPDATED");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    Notification notification =
        Notification.builder()
            .user(user)
            .title("Mật khẩu đã được thay đổi")
            .content(
                "Mật khẩu tài khoản của bạn đã được thay đổi thành công vào lúc "
                    + LocalDateTime.now().format(formatter))
            .isRead(false)
            .build();
    notificationRepository.save(notification);

    TokenPair tokenPair = tokenRedisService.getTokenPair(user.getUserId());
    if (tokenPair != null && tokenPair.getRefreshToken() != null) {
      try {
        Date refreshExp =
            SignedJWT.parse(tokenPair.getRefreshToken()).getJWTClaimsSet().getExpirationTime();
        tokenRedisService.blacklistToken(user.getUserId(), tokenPair, refreshExp);
      } catch (ParseException e) {
        log.warn("Không thể parse hạn refresh token để blacklist cho user {}", user.getUserId());
      }
    }
    tokenRedisService.deleteToken(user.getUserId());
  }

  // =====================================================================
  // HELPERS - OTP / SESSION
  // =====================================================================

  private static String generateOtp() {
    return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
  }

  private boolean otpMatches(String expected, String actual) {
    if (expected == null || actual == null) {
      return false;
    }
    String trimmed = actual.trim();
    if (expected.startsWith("$2a$") || expected.startsWith("$2b$") || expected.startsWith("$2y$")) {
      return passwordEncoder.matches(trimmed, expected);
    }
    return MessageDigest.isEqual(
        expected.getBytes(StandardCharsets.UTF_8), trimmed.getBytes(StandardCharsets.UTF_8));
  }

  private void assertCooldownPassed(LocalDateTime createdAt, String sessionKey) {
    if (createdAt == null) {
      return;
    }
    long elapsed = ChronoUnit.SECONDS.between(createdAt, LocalDateTime.now());
    if (elapsed < OTP_COOLDOWN_SECONDS) {
      log.warn("Session [{}] yêu cầu gửi lại OTP quá sớm ({}s)", sessionKey, elapsed);
      throw new AppException(ErrorCode.OTP_COOLDOWN_ACTIVE);
    }
  }

  private void assertOtpSendingAllowed(String clientIp, String target) {
    if (tokenRedisService.isOtpSendingBlockedForIp(clientIp)) {
      log.warn("IP [{}] bị chặn gửi OTP do vượt quá giới hạn", clientIp);
      throw new AppException(
          ErrorCode.OTP_SEND_LIMIT_EXCEEDED,
          "Địa chỉ IP của bạn đã gửi yêu cầu OTP quá số lần cho phép. Vui lòng thử lại sau 15 phút.");
    }
    if (tokenRedisService.isOtpSendingBlockedForTarget(target)) {
      log.warn("Target [{}] bị chặn gửi OTP do vượt quá giới hạn", target);
      throw new AppException(
          ErrorCode.OTP_SEND_LIMIT_EXCEEDED,
          "Số điện thoại hoặc Email này đã yêu cầu gửi mã OTP quá số lần cho phép. Vui lòng thử lại sau 15 phút.");
    }
  }

  private void recordOtpSendAttempt(String clientIp, String target) {
    tokenRedisService.recordOtpSendAttemptForIp(clientIp);
    tokenRedisService.recordOtpSendAttemptForTarget(target);
  }

  private static OtpChannel resolveChannel(String identity) {
    return identity.contains("@") ? OtpChannel.EMAIL : OtpChannel.PHONE;
  }

  private static String resolveTarget(OtpChannel channel, User user, String identity) {
    String target = channel == OtpChannel.EMAIL ? user.getEmail() : user.getPhone();
    return (target == null || target.isBlank()) ? identity : target;
  }

  // =====================================================================
  // HELPERS - USER
  // =====================================================================

  private User findUser(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));
  }

  private void assertNotLocked(User user) {
    if (user.getStatus() == UserStatus.LOCKED) {
      log.warn("Tài khoản userId {} đang bị khóa", user.getUserId());
      throw new AppException(ErrorCode.USER_LOCKED);
    }
  }

  private void assertUserNotExists(String email, String phone) {
    if (userRepository.countByEmailOrPhone(email, phone) > 0) {
      log.warn("Email {} hoặc SĐT {} đã tồn tại trên hệ thống", email, phone);
      throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
    }
  }

  private AppException failedLogin(String identity) {
    long fails = tokenRedisService.recordFailedLoginAttempt(identity);
    return new AppException(
        fails >= MAX_FAILED_LOGIN ? ErrorCode.TOO_MANY_REQUESTS : ErrorCode.UNAUTHENTICATED);
  }

  private static Notification buildNotification(User user, String title, String content) {
    return Notification.builder().user(user).title(title).content(content).isRead(false).build();
  }

  private static String maskTarget(OtpChannel channel, String target) {
    if (target == null || target.isBlank()) {
      return "***";
    }
    if (channel == OtpChannel.PHONE) {
      int len = target.length();
      if (len <= 4) return "****";
      if (len <= 7) return target.substring(0, 2) + "***" + target.substring(len - 2);
      return target.substring(0, 3) + "***" + target.substring(len - 4);
    }
    int at = target.indexOf('@');
    if (at < 0) {
      return target.length() > 3 ? target.substring(0, 3) + "***" : "***";
    }
    String name = target.substring(0, at);
    String domain = target.substring(at + 1);
    String maskedName =
        name.isEmpty()
            ? "***"
            : name.length() > 3 ? name.substring(0, 3) + "***" : name.charAt(0) + "***";
    return maskedName + "@" + domain;
  }

  // =====================================================================
  // HELPERS - JWT
  // =====================================================================

  AuthenticationResponse generateToken(User user) {
    String accessToken = buildToken(user, accessExpiration, TYPE_ACCESS);
    String refreshToken = buildToken(user, refreshExpiration, TYPE_REFRESH);
    try {
      Date refreshExp = SignedJWT.parse(refreshToken).getJWTClaimsSet().getExpirationTime();
      tokenRedisService.saveToken(user.getUserId(), accessToken, refreshToken, refreshExp);
    } catch (ParseException e) {
      log.error("Error parsing refresh token", e);
      throw new IllegalStateException(e);
    }
    return AuthenticationResponse.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .users(user)
        .build();
  }

  private String buildToken(User user, long validSeconds, String tokenType) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(user.getUserId().toString())
            .issuer(user.getEmail() != null ? user.getEmail() : user.getUserId().toString())
            .issueTime(new Date())
            .expirationTime(Date.from(Instant.now().plusSeconds(validSeconds)))
            .jwtID(UUID.randomUUID().toString())
            .claim("Scope", buildScope(user))
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .build();
    JWSObject jws =
        new JWSObject(new JWSHeader(JWSAlgorithm.HS512), new Payload(claims.toJSONObject()));
    try {
      jws.sign(new MACSigner(signedKey.getBytes(StandardCharsets.UTF_8)));
      return jws.serialize();
    } catch (JOSEException e) {
      log.error("Cannot create token", e);
      throw new IllegalStateException(e);
    }
  }

  private static String buildScope(User user) {
    return user.getRole() != null ? "ROLE_" + user.getRole() : "";
  }

  private SignedJWT verifyToken(String token, String expectedType)
      throws JOSEException, ParseException {
    SignedJWT signedJWT;
    try {
      signedJWT = SignedJWT.parse(token);
    } catch (ParseException e) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    JWSVerifier verifier = new MACVerifier(signedKey.getBytes(StandardCharsets.UTF_8));
    JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
    Date exp = claims.getExpirationTime();

    if (!signedJWT.verify(verifier)
        || exp == null
        || !exp.after(new Date())
        || !expectedType.equals(claims.getClaim(TOKEN_TYPE_CLAIM))) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    if (tokenRedisService.isTokenBlacklisted(
        Long.valueOf(signedJWT.getJWTClaimsSet().getSubject()), token)) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    return signedJWT;
  }
}
