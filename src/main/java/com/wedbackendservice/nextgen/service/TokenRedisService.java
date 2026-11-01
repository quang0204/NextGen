package com.wedbackendservice.nextgen.service;

import com.wedbackendservice.nextgen.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.dto.session.RegisterSessionData;
import com.wedbackendservice.nextgen.dto.session.ResetPasswordSessionData;
import com.wedbackendservice.nextgen.dto.session.TokenPair;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService {
    private static final String SESSION_PREFIX = "token:session:";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String OTP_PREFIX = "otp:session:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String REG_PREFIX = "reg_token_";
    private static final String RESET_PREFIX = "reset_token_";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration FAIL_LOCK_DURATION = Duration.ofMinutes(15);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ===== JWT SESSION =====

    public void saveToken(Long userId, String accessToken, String refreshToken, Date refreshExpiry) {
        TokenPair pair = TokenPair.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
        save(SESSION_PREFIX + userId, pair, refreshExpiry);
    }

    public TokenPair getTokenPair(Long userId) {
        return read(SESSION_PREFIX + userId, TokenPair.class);
    }

    public void deleteToken(Long userId) {
        redisTemplate.delete(SESSION_PREFIX + userId);
    }

    // ===== JWT BLACKLIST =====

    public void blacklistToken(Long userId, TokenPair tokenPair, Date refreshExpiry) {
        save(BLACKLIST_PREFIX + userId, tokenPair, refreshExpiry);
    }

    public boolean isTokenBlacklisted(Long userId, String token) {
        TokenPair pair = read(BLACKLIST_PREFIX + userId, TokenPair.class);
        if (pair == null) {
            return false;
        }
        return token.equals(pair.getAccessToken()) || token.equals(pair.getRefreshToken());
    }

    // ===== OTP SESSION =====

    public void saveOtpSession(String sessionToken, OtpSessionData sessionData, long ttlSeconds) {
        String key = OTP_PREFIX + sessionToken;
        try {
            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.info("Saved OTP session [{}] with TTL {}s", sessionToken, ttlSeconds);
        } catch (Exception e) {
            log.error("Error saving OTP session for key {}", key, e);
            throw new RuntimeException(e);
        }
    }

    public OtpSessionData getOtpSession(String sessionToken) {
        return read(OTP_PREFIX + sessionToken, OtpSessionData.class);
    }

    public void deleteOtpSession(String sessionToken) {
        redisTemplate.delete(OTP_PREFIX + sessionToken);
        log.info("Deleted OTP session [{}]", sessionToken);
    }

    // ===== REGISTRATION SESSION =====

    public void saveRegisterSession(String registrationToken, RegisterSessionData sessionData, long ttlSeconds) {
        String key = REG_PREFIX + registrationToken;
        try {
            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.info("Saved registration session [{}] with TTL {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error saving registration session for key {}", key, e);
            throw new RuntimeException(e);
        }
    }

    public RegisterSessionData getRegisterSession(String registrationToken) {
        return read(REG_PREFIX + registrationToken, RegisterSessionData.class);
    }

    public void deleteRegisterSession(String registrationToken) {
        redisTemplate.delete(REG_PREFIX + registrationToken);
        log.info("Deleted registration session [{}]", REG_PREFIX + registrationToken);
    }

    // ===== RESET PASSWORD SESSION =====

    public void saveResetPasswordSession(String resetToken, ResetPasswordSessionData sessionData, long ttlSeconds) {
        String key = RESET_PREFIX + resetToken;
        try {
            String json = objectMapper.writeValueAsString(sessionData);
            redisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttlSeconds));
            log.info("Saved reset password session [{}] with TTL {}s", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Error saving reset password session for key {}", key, e);
            throw new RuntimeException(e);
        }
    }

    public ResetPasswordSessionData getResetPasswordSession(String resetToken) {
        return read(RESET_PREFIX + resetToken, ResetPasswordSessionData.class);
    }

    public void deleteResetPasswordSession(String resetToken) {
        redisTemplate.delete(RESET_PREFIX + resetToken);
        log.info("Deleted reset password session [{}]", RESET_PREFIX + resetToken);
    }

    // ===== FAILED LOGIN ATTEMPTS RATE LIMITING =====

    public boolean isLoginBlocked(String identity) {
        String key = LOGIN_FAIL_PREFIX + identity;
        String val = redisTemplate.opsForValue().get(key);
        if (val == null) {
            return false;
        }
        try {
            return Integer.parseInt(val) >= MAX_FAILED_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public long recordFailedLoginAttempt(String identity) {
        String key = LOGIN_FAIL_PREFIX + identity;
        Long attempts = redisTemplate.opsForValue().increment(key);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(key, FAIL_LOCK_DURATION);
        }
        return attempts != null ? attempts : 1L;
    }

    public void clearFailedLoginAttempts(String identity) {
        redisTemplate.delete(LOGIN_FAIL_PREFIX + identity);
    }

    // ===== PRIVATE HELPERS =====

    private void save(String key, Object data, Date expiry) {
        try {
            long ttlMillis = expiry.getTime() - System.currentTimeMillis();
            if (ttlMillis <= 0) {
                return;
            }
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(key, json, Duration.ofMillis(ttlMillis));
        } catch (Exception e) {
            log.error("Error serializing data for key {}", key, e);
            throw new RuntimeException(e);
        }
    }

    private <T> T read(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing json for key {}", key, e);
            return null;
        }
    }
}
