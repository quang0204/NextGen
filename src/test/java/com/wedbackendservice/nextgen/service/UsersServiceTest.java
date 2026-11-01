package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.security.SecurityHelper;


import com.wedbackendservice.nextgen.dto.response.user.CreateAddressResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserProfileResponse;
import com.wedbackendservice.nextgen.dto.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.entity.UserAddress;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UsersServiceTest {

    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAddressRepository userAddressRepository;

    @InjectMocks
    private UsersService usersService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .userId(101L)
                .fullName("Trần Thị Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void testGetUserProfile_Success() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);

        UserProfileResponse resp = usersService.getUserProfile("Bearer valid-token");

        assertNotNull(resp);
        assertEquals(101L, resp.getUserId());
        assertEquals("Trần Thị Thu Huyền", resp.getFullName());
        assertEquals("thuhuyen.tran@gmail.com", resp.getEmail());
    }

    @Test
    void testUpdateUserProfile_Success() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);
        when(userRepository.countByEmailOrPhoneExcludingUser("new.email@gmail.com", "0911222333", 101L)).thenReturn(0L);

        UpdateUserProfileRequest req = UpdateUserProfileRequest.builder()
                .fullName("Trần Thu Huyền")
                .email("new.email@gmail.com")
                .phone("0911222333")
                .build();

        usersService.updateUserProfile("Bearer valid-token", req);

        verify(userRepository, times(1)).save(testUser);
        assertEquals("Trần Thu Huyền", testUser.getFullName());
        assertEquals("new.email@gmail.com", testUser.getEmail());
    }

    @Test
    void testUpdateUserProfile_DuplicateEmailOrPhone_ThrowsException() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);
        when(userRepository.countByEmailOrPhoneExcludingUser("duplicate@gmail.com", "0911222333", 101L)).thenReturn(1L);

        UpdateUserProfileRequest req = UpdateUserProfileRequest.builder()
                .fullName("Trần Thu Huyền")
                .email("duplicate@gmail.com")
                .phone("0911222333")
                .build();

        AppException ex = assertThrows(AppException.class, () ->
                usersService.updateUserProfile("Bearer valid-token", req));
        assertEquals(ErrorCode.USER_ALREADY_EXISTS, ex.getErrorCode());
    }

    @Test
    void testGetUserAddresses_Success() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);

        UserAddress addr = UserAddress.builder()
                .addressId(1L)
                .user(testUser)
                .recipientName("Trần Thị Thu Huyền")
                .phone("0987654321")
                .addressLine("Số 12, Phố Vọng")
                .isDefault(true)
                .build();

        when(userAddressRepository.findByUserIdOrderByIsDefaultDescAddressIdDesc(101L))
                .thenReturn(List.of(addr));

        List<UserAddressItemResponse> list = usersService.getUserAddresses("Bearer valid-token");

        assertEquals(1, list.size());
        assertEquals(1L, list.get(0).getAddressId());
        assertTrue(list.get(0).getIsDefault());
    }

    @Test
    void testCreateUserAddress_FirstAddress_SetsDefault() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);
        when(userAddressRepository.countByUserId(101L)).thenReturn(0L);

        UserAddress saved = UserAddress.builder()
                .addressId(3L)
                .user(testUser)
                .recipientName("Huyền")
                .phone("0987654321")
                .addressLine("Số 45 Lê Văn Lương")
                .isDefault(true)
                .build();

        when(userAddressRepository.save(any(UserAddress.class))).thenReturn(saved);

        UserAddressRequest req = UserAddressRequest.builder()
                .recipientName("Huyền")
                .phone("0987654321")
                .addressLine("Số 45 Lê Văn Lương")
                .isDefault(false)
                .build();

        CreateAddressResponse resp = usersService.createUserAddress("Bearer valid-token", req);

        assertNotNull(resp);
        assertEquals(3L, resp.getAddressId());
        verify(userAddressRepository, times(1)).resetDefaultAddressByUserId(101L);
    }

    @Test
    void testSetDefaultAddress_Success() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);

        UserAddress addr = UserAddress.builder()
                .addressId(2L)
                .user(testUser)
                .isDefault(false)
                .build();

        when(userAddressRepository.findByAddressIdAndUserId(2L, 101L)).thenReturn(Optional.of(addr));

        usersService.setDefaultAddress("Bearer valid-token", 2L);

        verify(userAddressRepository, times(1)).resetDefaultAddressByUserId(101L);
        assertTrue(addr.getIsDefault());
        verify(userAddressRepository, times(1)).save(addr);
    }

    @Test
    void testDeleteUserAddress_Default_PromotesRemainingAddress() {
        when(securityHelper.authenticate("Bearer valid-token")).thenReturn(testUser);

        UserAddress defaultAddr = UserAddress.builder()
                .addressId(1L)
                .user(testUser)
                .isDefault(true)
                .build();

        UserAddress otherAddr = UserAddress.builder()
                .addressId(2L)
                .user(testUser)
                .isDefault(false)
                .build();

        when(userAddressRepository.findByAddressIdAndUserId(1L, 101L)).thenReturn(Optional.of(defaultAddr));
        when(userAddressRepository.findLatestAddressByUserId(101L)).thenReturn(new ArrayList<>(List.of(otherAddr)));

        usersService.deleteUserAddress("Bearer valid-token", 1L);

        verify(userAddressRepository, times(1)).delete(defaultAddr);
        assertTrue(otherAddr.getIsDefault());
        verify(userAddressRepository, times(1)).save(otherAddr);
    }
}
