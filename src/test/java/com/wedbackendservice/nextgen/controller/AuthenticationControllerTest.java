package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.request.auth.*;
import com.wedbackendservice.nextgen.dto.response.UsersResponse;
import com.wedbackendservice.nextgen.dto.response.auth.*;
import com.wedbackendservice.nextgen.dto.response.user.UserMeResponse;
import com.wedbackendservice.nextgen.enums.OtpChannel;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.AuthenticationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testLoginInitiate_Success() throws Exception {
        LoginInitiateRequest req = LoginInitiateRequest.builder()
                .identity("0912345678")
                .password("Password123@")
                .build();

        LoginInitiateResponse resp = LoginInitiateResponse.builder()
                .channel(OtpChannel.PHONE)
                .targetMasked("091***5678")
                .sessionToken("sess_123")
                .expiresInSeconds(120)
                .cooldownSeconds(60)
                .build();

        when(authenticationService.initiateLogin(any(LoginInitiateRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login-initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.channel").value("PHONE"))
                .andExpect(jsonPath("$.data.sessionToken").value("sess_123"));
    }

    @Test
    void testLoginVerifyOtp_Success() throws Exception {
        LoginVerifyOtpRequest req = LoginVerifyOtpRequest.builder()
                .sessionToken("sess_123")
                .otpCode("123456")
                .build();

        LoginVerifyOtpResponse resp = LoginVerifyOtpResponse.builder()
                .tokenType("Bearer")
                .accessToken("acc_token")
                .refreshToken("ref_token")
                .expiresIn(3600L)
                .user(UsersResponse.builder()
                        .userId("101")
                        .fullName("Nguyễn Văn Hoàng")
                        .email("hoang@gmail.com")
                        .phone("0912345678")
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .build();

        when(authenticationService.verifyLoginOtp(any(LoginVerifyOtpRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/login-verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("acc_token"))
                .andExpect(jsonPath("$.data.user.userId").value("101"));
    }

    @Test
    void testRegisterInitiate_Success() throws Exception {
        RegisterInitiateRequest req = RegisterInitiateRequest.builder()
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang@gmail.com")
                .phone("0912345678")
                .password("Password123@")
                .confirmPassword("Password123@")
                .build();

        RegisterInitiateResponse resp = RegisterInitiateResponse.builder()
                .channel(OtpChannel.EMAIL)
                .targetEmail("hoa***@gmail.com")
                .registrationToken("reg_123")
                .expiresInSeconds(300)
                .cooldownSeconds(60)
                .build();

        when(authenticationService.initiateRegister(any(RegisterInitiateRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register-initiate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.channel").value("EMAIL"))
                .andExpect(jsonPath("$.data.registrationToken").value("reg_123"));
    }

    @Test
    void testRegisterVerifyOtp_Success() throws Exception {
        RegisterVerifyOtpRequest req = RegisterVerifyOtpRequest.builder()
                .registrationToken("reg_123")
                .otpCode("123456")
                .build();

        RegisterVerifyOtpResponse resp = RegisterVerifyOtpResponse.builder()
                .tokenType("Bearer")
                .accessToken("acc_token")
                .refreshToken("ref_token")
                .expiresIn(86400L)
                .user(UserRegisterResponse.builder()
                        .userId(105L)
                        .fullName("Nguyễn Văn Hoàng")
                        .email("hoang@gmail.com")
                        .phone("0912345678")
                        .role(Role.CUSTOMER)
                        .status(UserStatus.ACTIVE)
                        .build())
                .build();

        when(authenticationService.verifyRegisterOtp(any(RegisterVerifyOtpRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/register-verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.user.userId").value(105));
    }

    @Test
    void testRefreshToken_Success() throws Exception {
        RefreshTokenRequest req = RefreshTokenRequest.builder()
                .refreshToken("ref_token")
                .build();

        RefreshTokenResponse resp = RefreshTokenResponse.builder()
                .accessToken("new_acc_token")
                .refreshToken("new_ref_token")
                .expiresIn(3600L)
                .build();

        when(authenticationService.refreshAccessToken(any(RefreshTokenRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("new_acc_token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new_ref_token"));
    }

    @Test
    void testLogout_Success() throws Exception {
        LogoutRequest req = LogoutRequest.builder()
                .accessToken("acc_token")
                .build();

        doNothing().when(authenticationService).logout(any(LogoutRequest.class));

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đăng xuất tài khoản thành công"));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        ChangePasswordRequest req = ChangePasswordRequest.builder()
                .currentPassword("Old123@")
                .newPassword("New123@")
                .confirmNewPassword("New123@")
                .build();

        doNothing().when(authenticationService).changePassword(any(), any(ChangePasswordRequest.class));

        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header("Authorization", "Bearer valid_token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đổi mật khẩu thành công. Vui lòng ghi nhớ mật khẩu mới của bạn."));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        UserMeResponse resp = UserMeResponse.builder()
                .userId(101L)
                .fullName("Nguyễn Văn Hoàng")
                .email("hoang@gmail.com")
                .phone("0912345678")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(authenticationService.getCurrentUser(any())).thenReturn(resp);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer valid_token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn Hoàng"));
    }
}
