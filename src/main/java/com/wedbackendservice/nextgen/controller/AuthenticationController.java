package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.dto.response.user.UserMeResponse;

import com.wedbackendservice.nextgen.dto.request.auth.RefreshTokenRequest;
hhhhh
import com.nimbusds.jose.JOSEException;
import com.wedbackendservice.nextgen.dto.response.ApiResponse;
import com.wedbackendservice.nextgen.dto.response.auth.*;
import com.wedbackendservice.nextgen.dto.request.auth.*;
import com.wedbackendservice.nextgen.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    /**
     * API 1: Khởi tạo đăng nhập (Bước 1 Đăng nhập)
     */
    @PostMapping("/login-initiate")
    public ApiResponse<LoginInitiateResponse> loginInitiate(
            @Valid @RequestBody LoginInitiateRequest request) {
        return ApiResponse.<LoginInitiateResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Thông tin hợp lệ. Mã xác thực OTP đã được gửi.")
                .data(authenticationService.initiateLogin(request))
                .build();
    }

    /**
     * API 2: Xác thực mã OTP Đăng nhập (Bước 2 Đăng nhập)
     */
    @PostMapping("/login-verify-otp")
    public ApiResponse<LoginVerifyOtpResponse> loginVerifyOtp(
            @Valid @RequestBody LoginVerifyOtpRequest request) {
        return ApiResponse.<LoginVerifyOtpResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Đăng nhập thành công!")
                .data(authenticationService.verifyLoginOtp(request))
                .build();
    }

    /**
     * API 3: Gửi lại mã OTP Đăng nhập
     */
    @PostMapping("/login-resend-otp")
    public ApiResponse<ResendOtpResponse> loginResendOtp(
            @Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.<ResendOtpResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Mã OTP mới đã được gửi lại thành công.")
                .data(authenticationService.resendLoginOtp(request))
                .build();
    }

    /**
     * API 4: Đăng ký tài khoản - Bước 1
     */
    @PostMapping("/register-initiate")
    public ApiResponse<RegisterInitiateResponse> registerInitiate(
            @Valid @RequestBody RegisterInitiateRequest request) {
        return ApiResponse.<RegisterInitiateResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Mã xác thực kích hoạt tài khoản đã được gửi về Email của bạn.")
                .data(authenticationService.initiateRegister(request))
                .build();
    }

    /**
     * API 5: Xác thực OTP Đăng ký - Bước 2
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register-verify-otp")
    public ApiResponse<RegisterVerifyOtpResponse> registerVerifyOtp(
            @Valid @RequestBody RegisterVerifyOtpRequest request) {
        return ApiResponse.<RegisterVerifyOtpResponse>builder()
                .success(true)
                .statusCode(201)
                .message("Đăng ký và kích hoạt tài khoản thành công!")
                .data(authenticationService.verifyRegisterOtp(request))
                .build();
    }

    /**
     * API 6: Gửi lại mã OTP Đăng ký
     */
    @PostMapping("/register-resend-otp")
    public ApiResponse<RegisterResendOtpResponse> registerResendOtp(
            @Valid @RequestBody RegisterResendOtpRequest request) {
        return ApiResponse.<RegisterResendOtpResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Mã xác thực mới đã được gửi lại vào hòm thư Email.")
                .data(authenticationService.resendRegisterOtp(request))
                .build();
    }

    /**
     * API 7: Yêu cầu Quên mật khẩu
     */
    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.<ForgotPasswordResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Mã OTP khôi phục mật khẩu đã được gửi.")
                .data(authenticationService.forgotPassword(request))
                .build();
    }

    /**
     * API 8: Lưu mật khẩu mới
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới.")
                .build();
    }

    /**
     * API 9: Gửi lại OTP Quên mật khẩu
     */
    @PostMapping("/forgot-resend-otp")
    public ApiResponse<ForgotResendOtpResponse> forgotResendOtp(
            @Valid @RequestBody ForgotResendOtpRequest request) {
        return ApiResponse.<ForgotResendOtpResponse>builder()
                .success(true)
                .statusCode(200)
                .message("Mã OTP khôi phục mật khẩu mới đã được gửi lại.")
                .data(authenticationService.resendForgotPasswordOtp(request))
                .build();
    }

    /**
     * API 10: Cấp lại Cặp Token
     */
    @PostMapping("/refresh-token")
    public ApiResponse<RefreshTokenResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        return ApiResponse.<RefreshTokenResponse>builder()
                .success(true)
                .statusCode(200)
                .data(authenticationService.refreshAccessToken(request))
                .build();
    }

    /**
     * API 11: Đăng xuất
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @RequestBody(required = false) LogoutRequest request) throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Đăng xuất tài khoản thành công")
                .build();
    }

    /**
     * API 12: Lấy thông tin người dùng hiện tại
     */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String authHeader) throws ParseException, JOSEException {
        return ApiResponse.<UserMeResponse>builder()
                .success(true)
                .statusCode(200)
                .data(authenticationService.getCurrentUser())
                .build();
    }

    /**
     * API 13: Đổi mật khẩu
     */

    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) throws ParseException, JOSEException {
        authenticationService.changePassword(authHeader, request);
        return ApiResponse.<Void>builder()
                .success(true)
                .statusCode(200)
                .message("Đổi mật khẩu thành công. Vui lòng ghi nhớ mật khẩu mới của bạn.")
                .build();
    }
}
