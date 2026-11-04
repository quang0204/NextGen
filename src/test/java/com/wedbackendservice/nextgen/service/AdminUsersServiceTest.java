package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.response.PageResponse;
import com.wedbackendservice.nextgen.dto.response.admin.AdminUserItemResponse;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.AuditLogRepository;
import com.wedbackendservice.nextgen.repository.OrderRepository;
import com.wedbackendservice.nextgen.repository.UserAddressRepository;
import com.wedbackendservice.nextgen.repository.UserRepository;
import com.wedbackendservice.nextgen.security.SecurityHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUsersServiceTest {

    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserAddressRepository userAddressRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private AuditLogRepository auditLogRepository;
    @Mock
    private TokenRedisService tokenRedisService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminUsersService adminUsersService;

    @Test
    void getUsersList_InvalidPage_ThrowsAppException() {
        assertThatThrownBy(() -> adminUsersService.getUsersList(0, 10, null, null, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_NUMBER);
    }

    @Test
    void getUsersList_InvalidSize_ThrowsAppException() {
        assertThatThrownBy(() -> adminUsersService.getUsersList(1, 999, null, null, null, null, null))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PAGE_SIZE);
    }

    @Test
    void getUsersList_ValidPageAndSize_ExecutesSingleBatchStatsQuery() {
        User user1 = User.builder()
                .userId(1L)
                .fullName("User One")
                .email("one@example.com")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        User user2 = User.builder()
                .userId(2L)
                .fullName("User Two")
                .email("two@example.com")
                .role(Role.CUSTOMER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Page<User> pagedUsers = new PageImpl<>(List.of(user1, user2));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(pagedUsers);

        List<Object[]> batchStats = List.of(
                new Object[]{1L, 3L, new BigDecimal("1500000")},
                new Object[]{2L, 1L, new BigDecimal("200000")}
        );
        when(orderRepository.statsByUserIds(List.of(1L, 2L))).thenReturn(batchStats);

        PageResponse<AdminUserItemResponse> result = adminUsersService.getUsersList(1, 10, null, null, null, null, null);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTotalOrders()).isEqualTo(3);
        assertThat(result.getContent().get(0).getTotalSpent()).isEqualTo(new BigDecimal("1500000"));
        assertThat(result.getContent().get(1).getTotalOrders()).isEqualTo(1);
        assertThat(result.getContent().get(1).getTotalSpent()).isEqualTo(new BigDecimal("200000"));
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);

        // Verify batch stats was called exactly once with all user IDs (NO N+1 query)
        verify(orderRepository, times(1)).statsByUserIds(List.of(1L, 2L));
    }
}
