package com.wedbackendservice.nextgen.core.security;

import com.wedbackendservice.nextgen.core.exception.AppException;
import com.wedbackendservice.nextgen.core.exception.ErrorCode;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.enums.Role;
import com.wedbackendservice.nextgen.module.user.enums.UserStatus;
import com.wedbackendservice.nextgen.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityHelper {
  private final UserRepository userRepository;

  public User getCurrentUser() {
    Authentication authentication = getAuthentication();
    final Long userId;
    try {
      userId = Long.valueOf(authentication.getName());
    } catch (NumberFormatException e) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }

    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

    if (user.getStatus() == UserStatus.LOCKED) {
      throw new AppException(ErrorCode.USER_LOCKED);
    }

    return user;
  }

  public Authentication getAuthentication() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
    return authentication;
  }

  public User authenticateAdmin() {
    User user = getCurrentUser();
    if (user.getRole() != Role.ADMIN) {
      log.warn("User {} role {} không có quyền truy cập admin", user.getUserId(), user.getRole());
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return user;
  }

  public User authenticateStaffOrAdmin() {
    User user = getCurrentUser();
    if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
      log.warn("User {} role {} không có quyền STAFF/ADMIN", user.getUserId(), user.getRole());
      throw new AppException(ErrorCode.FORBIDDEN);
    }
    return user;
  }
}
