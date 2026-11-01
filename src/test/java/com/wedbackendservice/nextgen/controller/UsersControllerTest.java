package com.wedbackendservice.nextgen.controller;

import com.wedbackendservice.nextgen.security.SecurityHelper;


import com.wedbackendservice.nextgen.dto.response.user.CreateAddressResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserAddressItemResponse;
import com.wedbackendservice.nextgen.dto.response.user.UserProfileResponse;
import com.wedbackendservice.nextgen.dto.request.user.UpdateUserProfileRequest;
import com.wedbackendservice.nextgen.dto.request.user.UserAddressRequest;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.GlobalExceptionHandler;
import com.wedbackendservice.nextgen.service.UsersService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class UsersControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UsersService usersService;

    @InjectMocks
    private UsersController usersController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usersController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetProfile_Success() throws Exception {
        UserProfileResponse resp = UserProfileResponse.builder()
                .userId(101L)
                .fullName("Trần Thị Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .build();

        when(usersService.getUserProfile("Bearer valid-token")).thenReturn(resp);

        mockMvc.perform(get("/api/v1/users/profile")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(101))
                .andExpect(jsonPath("$.data.fullName").value("Trần Thị Thu Huyền"));
    }

    @Test
    void testUpdateProfile_Success() throws Exception {
        UpdateUserProfileRequest req = UpdateUserProfileRequest.builder()
                .fullName("Trần Thu Huyền")
                .email("thuhuyen.tran@gmail.com")
                .phone("0987654321")
                .build();

        doNothing().when(usersService).updateUserProfile(eq("Bearer valid-token"), any(UpdateUserProfileRequest.class));

        mockMvc.perform(put("/api/v1/users/profile")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật thông tin cá nhân thành công!"));
    }

    @Test
    void testGetUserAddresses_Success() throws Exception {
        UserAddressItemResponse addr = UserAddressItemResponse.builder()
                .addressId(1L)
                .recipientName("Trần Thị Thu Huyền")
                .phone("0987654321")
                .addressLine("Số 12, Phố Vọng")
                .isDefault(true)
                .build();

        when(usersService.getUserAddresses("Bearer valid-token")).thenReturn(List.of(addr));

        mockMvc.perform(get("/api/v1/users/addresses")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].addressId").value(1));
    }

    @Test
    void testCreateUserAddress_Success() throws Exception {
        UserAddressRequest req = UserAddressRequest.builder()
                .recipientName("Trần Thị Thu Huyền")
                .phone("0987654321")
                .addressLine("Số 45, Đường Lê Văn Lương")
                .isDefault(false)
                .build();

        when(usersService.createUserAddress(eq("Bearer valid-token"), any(UserAddressRequest.class)))
                .thenReturn(CreateAddressResponse.builder().addressId(3L).build());

        mockMvc.perform(post("/api/v1/users/addresses")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.addressId").value(3));
    }

    @Test
    void testUpdateUserAddress_Success() throws Exception {
        UserAddressRequest req = UserAddressRequest.builder()
                .recipientName("Trần Thị Thu Huyền")
                .phone("0987654321")
                .addressLine("Số 12, Phố Vọng")
                .isDefault(true)
                .build();

        doNothing().when(usersService).updateUserAddress(eq("Bearer valid-token"), eq(1L), any(UserAddressRequest.class));

        mockMvc.perform(put("/api/v1/users/addresses/1")
                        .header("Authorization", "Bearer valid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cập nhật địa chỉ thành công!"));
    }

    @Test
    void testSetDefaultAddress_Success() throws Exception {
        doNothing().when(usersService).setDefaultAddress("Bearer valid-token", 1L);

        mockMvc.perform(patch("/api/v1/users/addresses/1/default")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Đã đổi địa chỉ mặc định thành công!"));
    }

    @Test
    void testDeleteUserAddress_Success() throws Exception {
        doNothing().when(usersService).deleteUserAddress("Bearer valid-token", 1L);

        mockMvc.perform(delete("/api/v1/users/addresses/1")
                        .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Xóa địa chỉ thành công!"));
    }
}
