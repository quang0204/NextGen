package com.wedbackendservice.nextgen.module.user.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.core.security.SecurityHelper;
import com.wedbackendservice.nextgen.core.security.TokenRedisService;
import com.wedbackendservice.nextgen.module.notification.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.module.user.dto.client.request.UserAddressRequest;
import com.wedbackendservice.nextgen.module.user.dto.client.response.CreateAddressResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserAddressItemResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.entity.UserAddress;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.mapper.UsersMapper;
import com.wedbackendservice.nextgen.module.user.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import com.wedbackendservice.nextgen.module.user.service.notification.OtpNotificationService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsersServiceAddressTest {

  @Mock private SecurityHelper securityHelper;
  @Mock private UserRepository userRepository;
  @Mock private UserAddressRepository userAddressRepository;
  @Mock private TokenRedisService tokenRedisService;
  @Mock private OtpNotificationService otpNotificationService;
  @Mock private AuditLogRepository auditLogRepository;
  @Mock private UsersMapper usersMapper;
  @Mock private PasswordEncoder passwordEncoder;

  private UsersService usersService;
  private User currentUser;

  @BeforeEach
  void setUp() {
    usersService =
        new UsersService(
            securityHelper,
            userRepository,
            userAddressRepository,
            tokenRedisService,
            otpNotificationService,
            auditLogRepository,
            usersMapper,
            passwordEncoder);

    currentUser =
        User.builder()
            .userId(105L)
            .fullName("Tran Thu Huyen")
            .email("huyen.tran@phoneshop.vn")
            .phone("0987654321")
            .role(Role.CUSTOMER)
            .status(UserStatus.ACTIVE)
            .build();
    when(securityHelper.getCurrentUser()).thenReturn(currentUser);
  }

  @Test
  void createAndUpdateAddress_RejectsIncompleteAddressMissingWard() {
    when(userAddressRepository.countByUserId(105L)).thenReturn(0L);
    UserAddressRequest missingWardReq =
        UserAddressRequest.builder()
            .recipientName("Huyen")
            .phone("0987654321")
            .addressLine("Số 17 Pasteur, TP. Đà Lạt, Lâm Đồng")
            .build();

    assertThatThrownBy(() -> usersService.createUserAddress(missingWardReq))
        .isInstanceOf(AppException.class)
        .satisfies(
            ex -> {
              ErrorCode code = ((AppException) ex).getErrorCode();
              assertThat(code).isEqualTo(ErrorCode.INVALID_REQUEST);
              assertThat(code.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
            });

    UserAddressRequest blankSegmentReq =
        UserAddressRequest.builder()
            .recipientName("Huyen")
            .phone("0987654321")
            .addressLine("Số 17 Pasteur,  , TP. Đà Lạt, Lâm Đồng")
            .build();
    assertThatThrownBy(() -> usersService.createUserAddress(blankSegmentReq))
        .isInstanceOf(AppException.class)
        .satisfies(
            ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    UserAddress existing =
        UserAddress.builder()
            .addressId(21L)
            .user(currentUser)
            .recipientName("Huyen")
            .phone("0987654321")
            .addressLine("Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng")
            .isDefault(true)
            .build();
    when(userAddressRepository.findByAddressIdAndUserId(21L, 105L))
        .thenReturn(Optional.of(existing));
    assertThatThrownBy(() -> usersService.updateUserAddress(21L, missingWardReq))
        .isInstanceOf(AppException.class)
        .satisfies(
            ex -> assertThat(((AppException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));

    verify(userAddressRepository, never()).save(any());
  }

  @Test
  void createAndUpdateAddress_NormalizesAndSavesCompleteFourPartAddress() {
    when(userAddressRepository.countByUserId(105L)).thenReturn(1L);
    UserAddressRequest validReq =
        UserAddressRequest.builder()
            .recipientName(" Huyen ")
            .phone(" 0987654321 ")
            .addressLine(" Số 17 Pasteur ,  Phường 4 , TP. Đà Lạt , Lâm Đồng ")
            .isDefault(false)
            .build();

    UserAddress saved =
        UserAddress.builder()
            .addressId(22L)
            .user(currentUser)
            .recipientName("Huyen")
            .phone("0987654321")
            .addressLine("Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng")
            .isDefault(false)
            .build();
    when(userAddressRepository.save(any(UserAddress.class))).thenReturn(saved);
    when(usersMapper.toCreateAddressResponse(saved))
        .thenReturn(
            CreateAddressResponse.builder()
                .addressId(22L)
                .addressLine(saved.getAddressLine())
                .build());

    CreateAddressResponse createRes = usersService.createUserAddress(validReq);
    assertThat(createRes.getAddressLine())
        .isEqualTo("Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng");

    ArgumentCaptor<UserAddress> captor = ArgumentCaptor.forClass(UserAddress.class);
    verify(userAddressRepository).save(captor.capture());
    assertThat(captor.getValue().getAddressLine())
        .isEqualTo("Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng");

    when(userAddressRepository.findByAddressIdAndUserId(22L, 105L))
        .thenReturn(Optional.of(saved));
    when(usersMapper.toUserAddressItemResponse(saved))
        .thenReturn(
            UserAddressItemResponse.builder()
                .addressId(22L)
                .addressLine(saved.getAddressLine())
                .build());

    UserAddressItemResponse updateRes = usersService.updateUserAddress(22L, validReq);
    assertThat(updateRes.getAddressLine())
        .isEqualTo("Số 17 Pasteur, Phường 4, TP. Đà Lạt, Lâm Đồng");
  }
}
