package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;

import com.wedbackendservice.nextgen.dto.response.user.CreateAddressResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserProfileResponse;
import com.wedbackendservice.nextgen.dto.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.entity.UserAddress;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UsersService {

    private final SecurityHelper securityHelper;
    private final UserRepository userRepository;
    private final UserAddressRepository userAddressRepository;

    /**
     * API 1: Lấy thông tin hồ sơ cá nhân
     */
    public UserProfileResponse getUserProfile(String authHeader) {
        User user = securityHelper.authenticate(authHeader);
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .role(user.getRole())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * API 2: Cập nhật thông tin cá nhân
     */
    @Transactional
    public void updateUserProfile(String authHeader, UpdateUserProfileRequest request) {
        User user = securityHelper.authenticate(authHeader);

        String newEmail = request.getEmail().trim();
        String newPhone = request.getPhone().trim();

        if (userRepository.countByEmailOrPhoneExcludingUser(newEmail, newPhone, user.getUserId()) > 0) {
            log.warn("Email [{}] hoặc SĐT [{}] đã tồn tại ở tài khoản khác", newEmail, newPhone);
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(newEmail);
        user.setPhone(newPhone);

        userRepository.save(user);
    }

    /**
     * API 3: Danh sách sổ địa chỉ nhận hàng
     */
    public List<UserAddressItemResponse> getUserAddresses(String authHeader) {
        User user = securityHelper.authenticate(authHeader);
        List<UserAddress> addresses = userAddressRepository
                .findByUserIdOrderByIsDefaultDescAddressIdDesc(user.getUserId());

        return addresses.stream()
                .map(this::mapToAddressResponse)
                .toList();
    }

    /**
     * API 4: Thêm mới 1 địa chỉ giao hàng
     */
    @Transactional
    public CreateAddressResponse createUserAddress(String authHeader, UserAddressRequest request) {
        User user = securityHelper.authenticate(authHeader);

        long count = userAddressRepository.countByUserId(user.getUserId());
        boolean shouldBeDefault = (count == 0) || Boolean.TRUE.equals(request.getIsDefault());

        if (shouldBeDefault) {
            userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .recipientName(request.getRecipientName().trim())
                .phone(request.getPhone().trim())
                .addressLine(request.getAddressLine().trim())
                .isDefault(shouldBeDefault)
                .build();

        UserAddress saved = userAddressRepository.save(address);
        return CreateAddressResponse.builder()
                .addressId(saved.getAddressId())
                .build();
    }

    /**
     * API 5: Sửa địa chỉ giao hàng
     */
    @Transactional
    public void updateUserAddress(String authHeader, Long addressId, UserAddressRequest request) {
        User user = securityHelper.authenticate(authHeader);

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        if (Boolean.TRUE.equals(request.getIsDefault()) && !Boolean.TRUE.equals(address.getIsDefault())) {
            userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.getIsDefault())) {
            address.setIsDefault(false);
        }

        address.setRecipientName(request.getRecipientName().trim());
        address.setPhone(request.getPhone().trim());
        address.setAddressLine(request.getAddressLine().trim());

        userAddressRepository.save(address);
    }

    /**
     * API 6: Đặt địa chỉ làm mặc định
     */
    @Transactional
    public void setDefaultAddress(String authHeader, Long addressId) {
        User user = securityHelper.authenticate(authHeader);

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        userAddressRepository.resetDefaultAddressByUserId(user.getUserId());
        address.setIsDefault(true);

        userAddressRepository.save(address);
    }

    /**
     * API 7: Xóa địa chỉ khỏi sổ địa chỉ
     */
    @Transactional
    public void deleteUserAddress(String authHeader, Long addressId) {
        User user = securityHelper.authenticate(authHeader);

        UserAddress address = userAddressRepository.findByAddressIdAndUserId(addressId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        userAddressRepository.delete(address);

        if (wasDefault) {
            List<UserAddress> remaining = userAddressRepository.findLatestAddressByUserId(user.getUserId());
            if (!remaining.isEmpty()) {
                UserAddress nextDefault = remaining.get(0);
                nextDefault.setIsDefault(true);
                userAddressRepository.save(nextDefault);
            }
        }
    }

    private UserAddressItemResponse mapToAddressResponse(UserAddress address) {
        return UserAddressItemResponse.builder()
                .addressId(address.getAddressId())
                .recipientName(address.getRecipientName())
                .phone(address.getPhone())
                .addressLine(address.getAddressLine())
                .isDefault(address.getIsDefault())
                .build();
    }
}
