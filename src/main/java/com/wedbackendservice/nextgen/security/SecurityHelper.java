package com.wedbackendservice.nextgen.security;

import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityHelper {
    private final UserRepository userRepository;
    public User getCurrentUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        Long userId = Long.valueOf(authentication.getName());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        return user;
    }

    public Authentication getAuthentication() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }
        return authentication;
    }

    public User authenticateAdmin() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN) {
            log.warn("User {} role {} không có quyền truy cập admin", user.getUserId(), user.getRole());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public User authenticateStaffOrAdmin() {
        User user = getCurrentUser();
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            log.warn("User {} role {} không có quyền STAFF/ADMIN", user.getUserId(), user.getRole());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }
}
