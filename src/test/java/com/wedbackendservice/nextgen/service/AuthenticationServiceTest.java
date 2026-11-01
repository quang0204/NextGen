package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.auth.*;
import com.wedbackendservice.nextgen.dto.response.UsersResponse;
import com.wedbackendservice.nextgen.dto.response.auth.*;
import com.wedbackendservice.nextgen.dto.response.user.UserMeResponse;
import com.wedbackendservice.nextgen.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.dto.session.RegisterSessionData;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.OtpChannel;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.UsersMapper;
import com.wedbackendservice.nextgen.repository.*;
import com.wedbackendservice.nextgen.service.notification.OtpNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    private static final String SIGNED_KEY = "prH5KZ52uBeUkW8r83pntzBa7vaIqeXc0U7Lmsw59Kopq8wwcIVDk01eAew1XS97cwh3M8a3PHH6CVwVJkv9dA";

    @Mock
    private UserRepository userRepository;
    @Mock
    private CartRepository cartRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TokenRedisService tokenRedisService;
    @Mock
    private OtpNotificationService otpNotificationService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UsersMapper usersMapper;

    @InjectMocks
    private AuthenticationService authenticationService;

    private User activeUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authenticationService, "signedKey", SIGNED_KEY);
        ReflectionTestUtils.setField(authenticationService, "accessExpiration", 1800L);
        ReflectionTestUtils.setField(authenticationService, "refreshExpiration", 1800L);

        activeUser = User.builder()
                .userId(101L)
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .passwordHash("$2a$10$encodedPassword")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // =========================================================================
    // 1. Login Initiate
    // =========================================================================
    @Test
    void testLoginInitiate_Success_Phone() {
        LoginInitiateRequest req = LoginInitiateRequest.builder()
                .identity("0912345678")
                .password("Password123@")
                .build();

        when(tokenRedisService.isLoginBlocked("0912345678")).thenReturn(false);
        when(userRepository.findByIdentity("0912345678")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123@", activeUser.getPasswordHash())).thenReturn(true);

        LoginInitiateResponse resp = authenticationService.initiateLogin(req);

        assertNotNull(resp);
        assertEquals(OtpChannel.PHONE, resp.getChannel());
        assertEquals("091***5678", resp.getTargetMasked());
        assertNotNull(resp.getSessionToken());
        verify(tokenRedisService, times(1)).saveOtpSession(anyString(), any(OtpSessionData.class), eq(120L));
        verify(otpNotificationService, times(1)).sendOtp(eq(OtpChannel.PHONE), eq("0912345678"), anyString());
    }

    @Test
    void testLoginInitiate_UserNotFound_ThrowsException() {
        LoginInitiateRequest req = LoginInitiateRequest.builder()
                .identity("unknown@gmail.com")
                .password("Password123@")
                .build();

        when(tokenRedisService.isLoginBlocked("unknown@gmail.com")).thenReturn(false);
        when(userRepository.findByIdentity("unknown@gmail.com")).thenReturn(Optional.empty());
        when(tokenRedisService.recordFailedLoginAttempt("unknown@gmail.com")).thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.initiateLogin(req));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void testLoginInitiate_WrongPassword_ThrowsException() {
        LoginInitiateRequest req = LoginInitiateRequest.builder()
                .identity("0912345678")
                .password("WrongPassword")
                .build();

        when(tokenRedisService.isLoginBlocked("0912345678")).thenReturn(false);
        when(userRepository.findByIdentity("0912345678")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPassword", activeUser.getPasswordHash())).thenReturn(false);
        when(tokenRedisService.recordFailedLoginAttempt("0912345678")).thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.initiateLogin(req));
        assertEquals(ErrorCode.UNAUTHENTICATED, ex.getErrorCode());
    }

    @Test
    void testLoginInitiate_UserLocked_ThrowsException() {
        activeUser.setStatus(UserStatus.LOCKED);
        LoginInitiateRequest req = LoginInitiateRequest.builder()
                .identity("0912345678")
                .password("Password123@")
                .build();

        when(tokenRedisService.isLoginBlocked("0912345678")).thenReturn(false);
        when(userRepository.findByIdentity("0912345678")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("Password123@", activeUser.getPasswordHash())).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.initiateLogin(req));
        assertEquals(ErrorCode.USER_LOCKED, ex.getErrorCode());
    }

    // =========================================================================
    // 2. Login Verify OTP
    // =========================================================================
    @Test
    void testLoginVerifyOtp_Success() {
        LoginVerifyOtpRequest req = LoginVerifyOtpRequest.builder()
                .sessionToken("sess_123")
                .otpCode("123456")
                .build();

        OtpSessionData session = OtpSessionData.builder()
                .userId(101L)
                .otpCode("123456")
                .channel(OtpChannel.EMAIL)
                .target("hoang.nguyen@gmail.com")
                .createdAt(LocalDateTime.now())
                .build();

        when(tokenRedisService.getOtpSession("sess_123")).thenReturn(session);
        when(userRepository.findById(101L)).thenReturn(Optional.of(activeUser));
        when(usersMapper.usersResponse(activeUser)).thenReturn(UsersResponse.builder()
                .userId("101")
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        LoginVerifyOtpResponse resp = authenticationService.verifyLoginOtp(req);

        assertNotNull(resp);
        assertEquals("Bearer", resp.getTokenType());
        assertNotNull(resp.getAccessToken());
        assertNotNull(resp.getRefreshToken());
        assertEquals("101", resp.getUser().getUserId());
        verify(tokenRedisService, times(1)).deleteOtpSession("sess_123");
        verify(tokenRedisService, times(1)).saveToken(eq(101L), anyString(), anyString(), any());
    }

    @Test
    void testLoginVerifyOtp_InvalidOtp_ThrowsException() {
        LoginVerifyOtpRequest req = LoginVerifyOtpRequest.builder()
                .sessionToken("sess_123")
                .otpCode("999999")
                .build();

        OtpSessionData session = OtpSessionData.builder()
                .userId(101L)
                .otpCode("123456")
                .build();

        when(tokenRedisService.getOtpSession("sess_123")).thenReturn(session);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.verifyLoginOtp(req));
        assertEquals(ErrorCode.OTP_INVALID_OR_EXPIRED, ex.getErrorCode());
    }

    // =========================================================================
    // 3. Register Initiate & Verify
    // =========================================================================
    @Test
    void testRegisterInitiate_Success() {
        RegisterInitiateRequest req = RegisterInitiateRequest.builder()
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .password("Password123@")
                .confirmPassword("Password123@")
                .build();

        when(userRepository.countByEmailOrPhone("hoang.nguyen@gmail.com", "0912345678")).thenReturn(0L);
        when(passwordEncoder.encode("Password123@")).thenReturn("hashedPassword");

        RegisterInitiateResponse resp = authenticationService.initiateRegister(req);

        assertNotNull(resp);
        assertEquals(OtpChannel.EMAIL, resp.getChannel());
        assertEquals("hoang.nguyen@gmail.com", resp.getTargetEmail());
        verify(tokenRedisService, times(1)).saveRegisterSession(anyString(), any(RegisterSessionData.class), eq(300L));
    }

    @Test
    void testRegisterInitiate_DuplicateUser_ThrowsConflict() {
        RegisterInitiateRequest req = RegisterInitiateRequest.builder()
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .password("Password123@")
                .confirmPassword("Password123@")
                .build();

        when(userRepository.countByEmailOrPhone("hoang.nguyen@gmail.com", "0912345678")).thenReturn(1L);

        AppException ex = assertThrows(AppException.class, () -> authenticationService.initiateRegister(req));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void testRegisterVerifyOtp_Success() {
        RegisterVerifyOtpRequest req = RegisterVerifyOtpRequest.builder()
                .registrationToken("reg_123")
                .otpCode("123456")
                .build();

        RegisterSessionData session = RegisterSessionData.builder()
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .passwordHash("hashed")
                .otpCode("123456")
                .build();

        when(tokenRedisService.getRegisterSession("reg_123")).thenReturn(session);
        when(userRepository.countByEmailOrPhone("hoang.nguyen@gmail.com", "0912345678")).thenReturn(0L);
        when(userRepository.save(any(User.class))).thenReturn(activeUser);
        when(usersMapper.userRegisterResponse(any(User.class))).thenReturn(UserRegisterResponse.builder()
                .userId(101L)
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang.nguyen@gmail.com")
                .phone("0912345678")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build());

        RegisterVerifyOtpResponse resp = authenticationService.verifyRegisterOtp(req);

        assertNotNull(resp);
        assertEquals("Bearer", resp.getTokenType());
        assertEquals(101L, resp.getUser().getUserId());
        verify(cartRepository, times(1)).save(any());
        verify(notificationRepository, times(1)).save(any());
        verify(tokenRedisService, times(1)).deleteRegisterSession("reg_123");
    }

    // =========================================================================
    // 4. Change Password
    // =========================================================================
    @Test
    void testChangePassword_Success() throws Exception {
        AuthenticationResponse auth = authenticationService.generateToken(activeUser);

        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("OldPass123@")
                .newPassword("NewPass123@")
                .confirmNewPassword("NewPass123@")
                .build();

        when(userRepository.findById(101L)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("OldPass123@", activeUser.getPasswordHash())).thenReturn(true);
        when(passwordEncoder.matches("NewPass123@", activeUser.getPasswordHash())).thenReturn(false);
        when(passwordEncoder.encode("NewPass123@")).thenReturn("newHashedPassword");

        authenticationService.changePassword("Bearer " + auth.getAccessToken(), req);

        verify(userRepository, times(1)).save(activeUser);
        verify(auditLogRepository, times(1)).save(any());
    }

    // =========================================================================
    // 5. Get Current User
    // =========================================================================
    @Test
    void testGetCurrentUser_Success() throws Exception {
        when(userRepository.findById(101L)).thenReturn(Optional.of(activeUser));
        when(userAddressRepository.findByUserIdAndIsDefaultTrue(101L)).thenReturn(Optional.empty());

        AuthenticationResponse auth = authenticationService.generateToken(activeUser);
        UserMeResponse me = authenticationService.getCurrentUser("Bearer " + auth.getAccessToken());

        assertNotNull(me);
        assertEquals(101L, me.getUserId());
        assertEquals("Nguyễn Văn Hoàng", me.getFullName());
    }
}
