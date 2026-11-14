package com.wedbackendservice.nextgen.controller.client;

import com.nimbusds.jose.JOSEException;
import com.wedbackendservice.nextgen.dto.client.request.auth.*;
import com.wedbackendservice.nextgen.dto.common.ApiResponse;
import com.wedbackendservice.nextgen.dto.client.response.auth.*;
import com.wedbackendservice.nextgen.dto.client.response.user.UserMeResponse;
import com.wedbackendservice.nextgen.service.client.AuthenticationService;
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
     * API 1: Đăng nhập trực tiếp (Tài khoản + Mật khẩu)
     */
    @PostMapping({"/login", "/login-initiate"})
    public ApiResponse<LoginVerifyOtpResponse> loginInitiate(@Valid @RequestBody LoginInitiateRequest request) {
        return ApiResponse.ok("Đăng nhập thành công!", authenticationService.initiateLogin(request));
    }

    /**
     * API 2: Xác thực mã OTP Đăng nhập (Bước 2 Đăng nhập)
     */
    @PostMapping("/login-verify-otp")
    public ApiResponse<LoginVerifyOtpResponse> loginVerifyOtp(@Valid @RequestBody LoginVerifyOtpRequest request) {
        return ApiResponse.ok("Đăng nhập thành công!", authenticationService.verifyLoginOtp(request));
    }

    /**
     * API 3: Gửi lại mã OTP Đăng nhập
     */
    @PostMapping("/login-resend-otp")
    public ApiResponse<ResendOtpResponse> loginResendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok("Mã OTP mới đã được gửi lại thành công.", authenticationService.resendLoginOtp(request));
    }

    /**
     * API 4: Đăng ký tài khoản - Bước 1
     */
    @PostMapping("/register-initiate")
    public ApiResponse<RegisterInitiateResponse> registerInitiate(@Valid @RequestBody RegisterInitiateRequest request) {
        return ApiResponse.ok("Mã xác thực kích hoạt tài khoản đã được gửi về Email của bạn.", authenticationService.initiateRegister(request));
    }

    /**
     * API 5: Xác thực OTP Đăng ký - Bước 2
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register-verify-otp")
    public ApiResponse<RegisterVerifyOtpResponse> registerVerifyOtp(@Valid @RequestBody RegisterVerifyOtpRequest request) {
        return ApiResponse.created("Đăng ký và kích hoạt tài khoản thành công!", authenticationService.verifyRegisterOtp(request));
    }

    /**
     * API 6: Gửi lại mã OTP Đăng ký
     */
    @PostMapping("/register-resend-otp")
    public ApiResponse<RegisterResendOtpResponse> registerResendOtp(@Valid @RequestBody RegisterResendOtpRequest request) {
        return ApiResponse.ok("Mã xác thực mới đã được gửi lại vào hòm thư Email.", authenticationService.resendRegisterOtp(request));
    }

    /**
     * API 7: Yêu cầu Quên mật khẩu
     */
    @PostMapping("/forgot-password")
    public ApiResponse<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok("Mã OTP khôi phục mật khẩu đã được gửi.", authenticationService.forgotPassword(request));
    }

    /**
     * API 8: Lưu mật khẩu mới
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authenticationService.resetPassword(request);
        return ApiResponse.ok("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới.", null);
    }

    /**
     * API 9: Gửi lại OTP Quên mật khẩu
     */
    @PostMapping("/forgot-resend-otp")
    public ApiResponse<ForgotResendOtpResponse> forgotResendOtp(@Valid @RequestBody ForgotResendOtpRequest request) {
        return ApiResponse.ok("Mã OTP khôi phục mật khẩu mới đã được gửi lại.", authenticationService.resendForgotPasswordOtp(request));
    }

    /**
     * API 10: Cấp lại Cặp Token
     */
    @PostMapping("/refresh-token")
    public ApiResponse<RefreshTokenResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) throws ParseException, JOSEException {
        return ApiResponse.ok(authenticationService.refreshAccessToken(request));
    }

    /**
     * API 11: Đăng xuất
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() throws ParseException {
        authenticationService.logout();
        return ApiResponse.ok("Đăng xuất tài khoản thành công", null);
    }

    /**
     * API 12: Lấy thông tin người dùng hiện tại
     */
    @GetMapping("/me")
    public ApiResponse<UserMeResponse> getCurrentUser() {
        return ApiResponse.ok(authenticationService.getCurrentUser());
    }

    /**
     * API 13: Đổi mật khẩu
     */
    @PutMapping("/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authenticationService.changePassword(request);
        return ApiResponse.ok("Đổi mật khẩu thành công. Vui lòng ghi nhớ mật khẩu mới của bạn.", null);
    }
}
