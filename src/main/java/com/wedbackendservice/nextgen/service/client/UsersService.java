package com.wedbackendservice.nextgen.service.client;
import com.wedbackendservice.nextgen.service.shared.TokenRedisService;
import com.wedbackendservice.nextgen.dto.client.request.user.ProfileResendOtpRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.ProfileUpdateConfirmRequest;
import com.wedbackendservice.nextgen.dto.client.request.user.ProfileUpdateRequest;
import com.wedbackendservice.nextgen.dto.client.response.user.ProfileUpdateOtpResponse;
import com.wedbackendservice.nextgen.dto.session.ProfileUpdateSessionData;
import com.wedbackendservice.nextgen.enums.OtpChannel;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.wedbackendservice.nextgen.dto.client.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.dto.client.response.user.*;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.entity.UserAddress;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.UsersMapper;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.service.shared.notification.OtpNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private static final int MAX_ADDRESS_LIMIT = 10;
    private static final int OTP_EXPIRE_SECONDS = 120;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final long PROFILE_UPDATE_TTL_SECONDS = 900;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;
    private final TokenRedisService tokenRedisService;
    private final OtpNotificationService otpNotificationService;
    private final AuditLogRepository auditLogRepository;
    private final UsersMapper usersMapper;

    /**
     * API 2.1: Lấy thông tin hồ sơ cá nhân
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile() {
        User user = securityHelper.getCurrentUser();
        return usersMapper.toUserProfileResponse(user);
    }
    /**
     * 1. POST /api/v1/users/profile/update-request
     * Gửi thông tin cá nhân mới cần cập nhật, kiểm tra trùng lặp Email/SĐT, sinh OTP & gửi email
     */
    public ProfileUpdateOtpResponse requestProfileUpdate(ProfileUpdateRequest request) {
        User user = securityHelper.getCurrentUser();

        String newFullName = request.getFullName().trim();
        String newPhone = request.getPhone().trim();
        String newEmail = request.getEmail().trim().toLowerCase();

        String currentFullName = user.getFullName() != null ? user.getFullName().trim() : "";
        String currentPhone = user.getPhone() != null ? user.getPhone().trim() : "";
        String currentEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";

        boolean nameChanged = !newFullName.equals(currentFullName);
        boolean phoneChanged = !newPhone.equals(currentPhone);
        boolean emailChanged = !newEmail.equalsIgnoreCase(currentEmail);

        // Bắt buộc phải có ít nhất 1 trong 3 thông tin thay đổi
        if (!nameChanged && !phoneChanged && !emailChanged) {
            throw new AppException(ErrorCode.INVALID_REQUEST, "Thông tin cá nhân mới không có sự thay đổi so với hiện tại!");
        }

        // Kiểm tra trùng lặp SĐT với tài khoản khác
        if (phoneChanged && userRepository.countByPhoneExcludingUser(newPhone, user.getUserId()) > 0) {
            throw new AppException(ErrorCode.DUPLICATE_PHONE, "Số điện thoại '" + newPhone + "' đã được đăng ký bởi một tài khoản khác!");
        }

        // Kiểm tra trùng lặp Email với tài khoản khác
        if (emailChanged && userRepository.countByEmailExcludingUser(newEmail, user.getUserId()) > 0) {
            throw new AppException(ErrorCode.DUPLICATE_EMAIL, "Email '" + newEmail + "' đã được sử dụng bởi một tài khoản khác!");
        }

        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        String otpSessionToken = "otp_profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Instant now = Instant.now();

        ProfileUpdateSessionData sessionData = ProfileUpdateSessionData.builder()
                .userId(user.getUserId())
                .fullName(newFullName)
                .phone(newPhone)
                .email(newEmail)
                .otpCode(otpCode)
                .createdAt(now)
                .lastSentAt(now)
                .otpExpiresAt(now.plusSeconds(OTP_EXPIRE_SECONDS))
                .build();

        tokenRedisService.saveProfileUpdateSession(otpSessionToken, sessionData, PROFILE_UPDATE_TTL_SECONDS);
        otpNotificationService.sendOtp(OtpChannel.EMAIL, newEmail, otpCode);
        log.info("Sent profile update OTP to email {} of user {}", newEmail, user.getUserId());

        return ProfileUpdateOtpResponse.builder()
                .otpSessionToken(otpSessionToken)
                .targetEmailMasked(maskEmail(newEmail))
                .expiresInSeconds(OTP_EXPIRE_SECONDS)
                .resendCooldownSeconds(RESEND_COOLDOWN_SECONDS)
                .build();
    }

    /**
     * 2. POST /api/v1/users/profile/update-confirm
     * Nhập mã OTP 6 số, đối chiếu khớp mã, lưu vào Users, ghi AuditLog & trả về profile mới nhất
     */
    @Transactional
    public UserProfileResponse confirmProfileUpdate(ProfileUpdateConfirmRequest request) {
        User user = securityHelper.getCurrentUser();

        ProfileUpdateSessionData session = tokenRedisService.getProfileUpdateSession(request.getOtpSessionToken());
        if (session == null || !session.getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND, "Phiên xác thực không tồn tại hoặc đã được xử lý trước đó!");
        }

        if (Instant.now().isAfter(session.getOtpExpiresAt())) {
            throw new AppException(ErrorCode.OTP_EXPIRED, "Mã xác thực OTP đã hết hạn hiệu lực. Vui lòng nhấn gửi lại mã mới!");
        }

        if (!session.getOtpCode().equals(request.getOtpCode())) {
            throw new AppException(ErrorCode.INVALID_OTP_CODE, "Mã xác thực OTP không chính xác. Vui lòng kiểm tra lại hộp thư email!");
        }

        String oldJson = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                escapeJson(user.getFullName()), escapeJson(user.getPhone()), escapeJson(user.getEmail()));

        user.setFullName(session.getFullName());
        user.setPhone(session.getPhone());
        user.setEmail(session.getEmail());
        User savedUser = userRepository.save(user);

        String newJson = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                escapeJson(session.getFullName()), escapeJson(session.getPhone()), escapeJson(session.getEmail()));

        auditLogRepository.log(savedUser, "UPDATE_PROFILE", "Users", savedUser.getUserId(), oldJson, newJson);
        tokenRedisService.deleteProfileUpdateSession(request.getOtpSessionToken());
        log.info("User {} successfully confirmed profile update", savedUser.getUserId());

        return usersMapper.toUserProfileResponse(savedUser);
    }

    /**
     * 3. POST /api/v1/users/profile/resend-otp
     * Gửi lại mã OTP mới sau cooldown 60s
     */
    public ProfileUpdateOtpResponse resendProfileUpdateOtp(ProfileResendOtpRequest request) {
        User user = securityHelper.getCurrentUser();

        ProfileUpdateSessionData session = tokenRedisService.getProfileUpdateSession(request.getOtpSessionToken());
        if (session == null || !session.getUserId().equals(user.getUserId())) {
            throw new AppException(ErrorCode.SESSION_NOT_FOUND, "Phiên xác thực không tồn tại hoặc đã được xử lý trước đó!");
        }

        Instant now = Instant.now();
        long secondsElapsed = Duration.between(session.getLastSentAt(), now).getSeconds();
        if (secondsElapsed < RESEND_COOLDOWN_SECONDS) {
            long remainingSeconds = RESEND_COOLDOWN_SECONDS - secondsElapsed;
            throw new AppException(ErrorCode.RESEND_COOLDOWN_ACTIVE,
                    "Vui lòng chờ " + remainingSeconds + " giây nữa trước khi bấm gửi lại mã!");
        }

        String newOtpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        session.setOtpCode(newOtpCode);
        session.setLastSentAt(now);
        session.setOtpExpiresAt(now.plusSeconds(OTP_EXPIRE_SECONDS));

        tokenRedisService.saveProfileUpdateSession(request.getOtpSessionToken(), session, PROFILE_UPDATE_TTL_SECONDS);
        otpNotificationService.sendOtp(OtpChannel.EMAIL, session.getEmail(), newOtpCode);
        log.info("Resent profile update OTP to email of user {}", user.getUserId());

        return ProfileUpdateOtpResponse.builder()
                .otpSessionToken(request.getOtpSessionToken())
                .targetEmailMasked(maskEmail(session.getEmail()))
                .expiresInSeconds(OTP_EXPIRE_SECONDS)
                .resendCooldownSeconds(RESEND_COOLDOWN_SECONDS)
                .build();
    }
    private String escapeJson(String input) {
        if (input == null) return "";
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * API 2.3: Danh sách sổ địa chỉ nhận hàng
     */
    @Transactional(readOnly = true)
    public List<UserAddressItemResponse> getUserAddresses() {
        User user = securityHelper.getCurrentUser();
        List<UserAddress> addresses = userAddressRepository
                .findByUserIdOrderByIsDefaultDescAddressIdDesc(user.getUserId());

        return addresses.stream()
                .map(usersMapper::toUserAddressItemResponse)
                .toList();
    }

    /**
     * API 2.4: Thêm mới 1 địa chỉ giao hàng
     */
    @Transactional
    public CreateAddressResponse createUserAddress(UserAddressRequest request) {
        User user = securityHelper.getCurrentUser();
        long currentCount = userAddressRepository.countByUserId(user.getUserId());

        if (currentCount >= MAX_ADDRESS_LIMIT) {
            throw new AppException(ErrorCode.ADDRESS_LIMIT_EXCEEDED);
        }

        boolean shouldBeDefault = currentCount == 0 || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
        }

        UserAddress userAddress = userAddressRepository.save(UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .addressLine(request.getAddressLine().trim())
                .isDefault(shouldBeDefault)
                .build());

        return usersMapper.toCreateAddressResponse(userAddress);
    }

    /**
     * API 2.5: Sửa địa chỉ giao hàng
     */
    @Transactional
    public UserAddressItemResponse updateUserAddress(Long addressId, UserAddressRequest request) {
        User user = securityHelper.getCurrentUser();

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            userAddressRepository.setDefaultAddressAtomic(user.getUserId(), addressId);
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault()) && Boolean.TRUE.equals(address.getIsDefault())) {
            throw new AppException(ErrorCode.CANNOT_UNSET_DEFAULT_ADDRESS);
        }

        // Đã sửa lỗi: Cập nhật trực tiếp lên entity address đã query thay vì tạo mới entity rác
        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddressLine(request.getAddressLine().trim());

        UserAddress saved = userAddressRepository.save(address);
        return usersMapper.toUserAddressItemResponse(saved);
    }

    /**
     * API 2.6: Đặt địa chỉ làm mặc định
     */
    @Transactional
    public SetDefaultAddressResponse setDefaultAddress(Long addressId) {
        User user = securityHelper.getCurrentUser();

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        userAddressRepository.setDefaultAddressAtomic(user.getUserId(), addressId);
        address.setIsDefault(true);

        return usersMapper.toSetDefaultAddressResponse(address);
    }

    /**
     * API 2.7: Xóa địa chỉ khỏi sổ địa chỉ
     */
    @Transactional
    public DeleteAddressResponse deleteUserAddress(Long addressId) {
        User user = securityHelper.getCurrentUser();

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        long count = userAddressRepository.countByUserId(user.getUserId());
        if (Boolean.TRUE.equals(address.getIsDefault()) && count > 1) {
            throw new AppException(ErrorCode.CANNOT_DELETE_DEFAULT_ADDRESS);
        }

        userAddressRepository.delete(address);

        return DeleteAddressResponse.builder()
                .deletedAddressId(addressId)
                .build();
    }
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "";
        }
        String[] parts = email.split("@", 2);
        String local = parts[0];
        String domain = parts[1];
        int visibleLen = local.length() <= 4 ? Math.min(2, local.length()) : 4;
        return local.substring(0, visibleLen) + "****@" + domain;
    }
}
