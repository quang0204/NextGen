package com.wedbackendservice.nextgen.security;

import com.wedbackendservice.nextgen.service.TokenRedisService;

import com.nimbusds.jose.JOSEException;
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
import lombok.RequiredArgsConstructor;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
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

    private final TokenRedisService tokenRedisService;
    private final UserRepository userRepository;

    public User authenticate(String authHeader) {
        String token = extractBearer(authHeader);
        if (token == null) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }

        SignedJWT signedJWT = verifyToken(token, TYPE_ACCESS);
        Long userId = extractUserId(signedJWT);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXIT));

        if (user.getStatus() == UserStatus.LOCKED) {
            throw new AppException(ErrorCode.USER_LOCKED);
        }

        return user;
    }

    public User authenticateAdmin(String authHeader) {
        User user = authenticate(authHeader);
        if (user.getRole() != Role.ADMIN) {
            log.warn("User {} role {} không có quyền truy cập admin", user.getUserId(), user.getRole());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public User authenticateStaffOrAdmin(String authHeader) {
        User user = authenticate(authHeader);
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            log.warn("User {} role {} không có quyền STAFF/ADMIN", user.getUserId(), user.getRole());
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    public SignedJWT verifyToken(String token, String expectedType) {
        SignedJWT signedJWT;
        try {
            signedJWT = SignedJWT.parse(token);
            JWSVerifier verifier = new MACVerifier(signedKey.getBytes(StandardCharsets.UTF_8));
            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
            Date exp = claims.getExpirationTime();

            if (!signedJWT.verify(verifier)
                    || exp == null || !exp.after(new Date())
                    || !expectedType.equals(claims.getClaim(TOKEN_TYPE_CLAIM))) {
                throw new AppException(ErrorCode.SESSION_EXPIRED);
            }
            if (tokenRedisService.isTokenBlacklisted(extractUserId(signedJWT), token)) {
                throw new AppException(ErrorCode.SESSION_EXPIRED);
            }
        } catch (ParseException | JOSEException e) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }
        return signedJWT;
    }

    public Long extractUserId(SignedJWT jwt) {
        try {
            return Long.valueOf(jwt.getJWTClaimsSet().getSubject());
        } catch (ParseException | NumberFormatException e) {
            throw new AppException(ErrorCode.SESSION_EXPIRED);
        }
    }

    public String extractBearer(String authHeader) {
        return (authHeader != null && authHeader.startsWith(BEARER_PREFIX))
                ? authHeader.substring(BEARER_PREFIX.length()).trim()
                : null;
    }
}
