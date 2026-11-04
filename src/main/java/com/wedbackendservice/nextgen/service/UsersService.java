package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.dto.response.user.*;
import com.wedbackendservice.nextgen.entity.AuditLog;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.entity.UserAddress;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.mapper.UsersMapper;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import com.wedbackendservice.nextgen.service.notification.OtpNotificationService;
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
    //
//    /**
//     * API gửi OTP xác nhận cập nhật Email/Phone
//     */
//    public void sendProfileUpdateOtp(String authHeader) {
//        User user = securityHelper.authenticate(authHeader);
//        String otpCode = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
//        String sessionKey = "profile_otp:" + user.getUserId();
//
//        tokenRedisService.saveOtpSession(sessionKey, OtpSessionData.builder()
//                .userId(user.getUserId())
//                .channel(OtpChannel.EMAIL)
//                .target(user.getEmail())
//                .otpCode(otpCode)
//                .createdAt(LocalDateTime.now())
//                .build(), OTP_EXPIRE_SECONDS);
//
//        otpNotificationService.sendOtp(OtpChannel.EMAIL, user.getEmail(), otpCode);
//        log.info("Sent profile update OTP to user {}", user.getUserId());
//    }

    /**
     * API 2.2: Cập nhật thông tin cá nhân (họ tên, email, số điện thoại)
     */
    @Transactional
    public UserProfileResponse updateUserProfile(UpdateUserProfileRequest request) {
        User user = securityHelper.getCurrentUser();

        String newFullName = request.getFullName().trim();
        String newPhone = request.getPhone().trim();
        String newEmail = request.getEmail().trim().toLowerCase();

        boolean nameChanged = !newFullName.equals(user.getFullName());
        boolean phoneChanged = !newPhone.equals(user.getPhone());
        boolean emailChanged = !newEmail.equalsIgnoreCase(user.getEmail());

        // 1. Kiểm tra trùng lặp Số điện thoại với tài khoản khác
        if (phoneChanged && userRepository.countByPhoneExcludingUser(newPhone, user.getUserId()) > 0) {
            log.warn("SĐT [{}] đã tồn tại ở tài khoản khác", newPhone);
            throw new AppException(ErrorCode.PHONE_ALREADY_EXISTS, "Số điện thoại '" + newPhone + "' đã được đăng ký bởi tài khoản khác!");
        }

        // 2. Kiểm tra trùng lặp Email với tài khoản khác
        if (emailChanged && userRepository.countByEmailExcludingUser(newEmail, user.getUserId()) > 0) {
            log.warn("Email [{}] đã tồn tại ở tài khoản khác", newEmail);
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS, "Địa chỉ Email '" + newEmail + "' đã được sử dụng bởi tài khoản khác!");
        }

        // 3. Thực hiện cập nhật nếu có thay đổi (Đã sửa lỗi không lưu entity hiện tại)
        if (nameChanged || phoneChanged || emailChanged) {
            String oldJson = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                    escapeJson(user.getFullName()), escapeJson(user.getPhone()), escapeJson(user.getEmail()));

            // Cập nhật trực tiếp trên đối tượng quản lý bởi Persistence Context
            user.setFullName(newFullName);
            user.setPhone(newPhone);
            user.setEmail(newEmail);
            userRepository.save(user);

            String newJson = String.format("{\"fullName\":\"%s\",\"phone\":\"%s\",\"email\":\"%s\"}",
                    escapeJson(newFullName), escapeJson(newPhone), escapeJson(newEmail));

            // 4. Ghi vết kiểm toán vào Audit_Logs
            auditLogRepository.save(AuditLog.builder()
                    .user(user)
                    .action("UPDATE_PROFILE")
                    .entityName("Users")
                    .entityId(user.getUserId())
                    .oldValue(oldJson)
                    .newValue(newJson)
                    .build());
        }

        return usersMapper.toUserProfileResponse(user);
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
            userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
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

        userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
        address.setIsDefault(true);

        UserAddress userAddress = userAddressRepository.save(address);

        return usersMapper.toSetDefaultAddressResponse(userAddress);
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
}