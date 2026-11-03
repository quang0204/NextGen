package com.wedbackendservice.nextgen.security;

import com.wedbackendservice.nextgen.service.TokenRedisService;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.enums.Role;
import com.wedbackendservice.nextgen.enums.UserStatus;
import com.wedbackendservice.nextgen.exception.AppException;
import com.wedbackendservice.nextgen.exception.ErrorCode;
import com.wedbackendservice.nextgen.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityHelper {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TYPE_ACCESS = "access";

    @NonFinal
    @Value("${jwt.signed-key}")
    protected String signedKey;

    private final UserRepository userRepository;
    private final TokenRedisService tokenRedisService;
    private final HttpServletRequest httpServletRequest;

    public User getCurrentUser() {
        Long userId = null;

        // 1. Trích xuất và xác thực Bearer token từ Authorization Header nếu có
        if (httpServletRequest != null) {
            String authHeader = httpServletRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                try {
                    SignedJWT signedJWT = SignedJWT.parse(token);
                    JWSVerifier verifier = new MACVerifier(signedKey.getBytes(StandardCharsets.UTF_8));
                    if (signedJWT.verify(verifier)) {
                        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
                        Date exp = claims.getExpirationTime();
                        if (exp != null && exp.after(new Date())) {
                            Long parsedId = Long.valueOf(claims.getSubject());
                            if (tokenRedisService == null || !tokenRedisService.isTokenBlacklisted(parsedId, token)) {
                                userId = parsedId;
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Parse JWT từ Authorization header thất bại: {}", e.getMessage());
                }
            }
        }

        // 2. Nếu chưa có từ header, kiểm tra SecurityContextHolder
        if (userId == null) {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && !"anonymousUser".equals(authentication.getName())) {
                try {
                    userId = Long.valueOf(authentication.getName());
                } catch (NumberFormatException ignored) {}
            }
        }

        if (userId == null) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

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
