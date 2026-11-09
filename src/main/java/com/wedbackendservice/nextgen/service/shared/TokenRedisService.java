package com.wedbackendservice.nextgen.service.shared;

import com.wedbackendservice.nextgen.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.dto.session.ProfileUpdateSessionData;
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

    private static final String SESSION_PREFIX   = "token:session:";
    private static final String BLACKLIST_PREFIX = "token:blacklist:";
    private static final String OTP_PREFIX       = "otp:session:";
    private static final String LOGIN_FAIL_PREFIX = "login:fail:";
    private static final String REG_PREFIX       = "reg_token_";
    private static final String RESET_PREFIX     = "reset_token_";
    private static final String PROFILE_UPDATE_PREFIX = "profile_update_session:";
    private static final String OTP_RATE_IP_PREFIX     = "otp:rate:ip:";
    private static final String OTP_RATE_TARGET_PREFIX = "otp:rate:target:";
    private static final String OTP_FAIL_PREFIX        = "otp:fail:";
    private static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int MAX_OTP_SEND_PER_IP        = 10;
    public static final int MAX_OTP_SEND_PER_TARGET    = 5;
    public static final int MAX_OTP_FAIL_ATTEMPTS       = 5;
    private static final Duration FAIL_LOCK_DURATION   = Duration.ofMinutes(15);
    private static final Duration OTP_RATE_WINDOW      = Duration.ofMinutes(15);
    private static final Duration OTP_FAIL_TTL         = Duration.ofSeconds(120);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    // ===== JWT SESSION / BLACKLIST =====

    public void saveToken(Long userId, String accessToken, String refreshToken, Date refreshExpiry) {
        save(SESSION_PREFIX + userId, new TokenPair(accessToken, refreshToken), expiryToTtl(refreshExpiry));
    }

    public TokenPair getTokenPair(Long userId) {
        return read(SESSION_PREFIX + userId, TokenPair.class);
    }

    public void deleteToken(Long userId) {
        delete(SESSION_PREFIX + userId);
    }

    public void blacklistToken(Long userId, TokenPair pair, Date refreshExpiry) {
        save(BLACKLIST_PREFIX + userId, pair, expiryToTtl(refreshExpiry));
    }

    public boolean isTokenBlacklisted(Long userId, String token) {
        TokenPair pair = read(BLACKLIST_PREFIX + userId, TokenPair.class);
        return pair != null && (token.equals(pair.getAccessToken()) || token.equals(pair.getRefreshToken()));
    }

    // ===== OTP / REGISTER / RESET SESSION =====

    public void saveOtpSession(String token, OtpSessionData data, long ttlSeconds) {
        save(OTP_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
    }
    public OtpSessionData getOtpSession(String token) { return read(OTP_PREFIX + token, OtpSessionData.class); }
    public void deleteOtpSession(String token)        { delete(OTP_PREFIX + token); }

    public void saveRegisterSession(String token, RegisterSessionData data, long ttlSeconds) {
        save(REG_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
    }
    public RegisterSessionData getRegisterSession(String token) { return read(REG_PREFIX + token, RegisterSessionData.class); }
    public void deleteRegisterSession(String token)             { delete(REG_PREFIX + token); }

    public void saveResetPasswordSession(String token, ResetPasswordSessionData data, long ttlSeconds) {
        save(RESET_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
    }
    public ResetPasswordSessionData getResetPasswordSession(String token) { return read(RESET_PREFIX + token, ResetPasswordSessionData.class); }
    public void deleteResetPasswordSession(String token)                  { delete(RESET_PREFIX + token); }

    public void saveProfileUpdateSession(String token, ProfileUpdateSessionData data, long ttlSeconds) {
        save(PROFILE_UPDATE_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
    }
    public ProfileUpdateSessionData getProfileUpdateSession(String token) { return read(PROFILE_UPDATE_PREFIX + token, ProfileUpdateSessionData.class); }
    public void deleteProfileUpdateSession(String token)                  { delete(PROFILE_UPDATE_PREFIX + token); }

    // ===== RATE LIMIT LOGIN =====

    public boolean isLoginBlocked(String identity) {
        String val = redisTemplate.opsForValue().get(LOGIN_FAIL_PREFIX + identity);
        try {
            return val != null && Integer.parseInt(val) >= MAX_FAILED_ATTEMPTS;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public long recordFailedLoginAttempt(String identity) {
        String key = LOGIN_FAIL_PREFIX + identity;
        Long attempts = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, FAIL_LOCK_DURATION); // set mỗi lần, tránh key không bao giờ hết hạn
        return attempts != null ? attempts : 1L;
    }

    public void clearFailedLoginAttempts(String identity) {
        delete(LOGIN_FAIL_PREFIX + identity);
    }

    // ===== RATE LIMIT OTP SENDING (IP & TARGET) =====

    public boolean isOtpSendingBlockedForIp(String ip) {
        if (ip == null || ip.isBlank()) return false;
        String val = redisTemplate.opsForValue().get(OTP_RATE_IP_PREFIX + ip);
        try {
            return val != null && Integer.parseInt(val) >= MAX_OTP_SEND_PER_IP;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public long recordOtpSendAttemptForIp(String ip) {
        if (ip == null || ip.isBlank()) return 1L;
        String key = OTP_RATE_IP_PREFIX + ip;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, OTP_RATE_WINDOW);
        } else {
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl < 0) {
                redisTemplate.expire(key, OTP_RATE_WINDOW);
            }
        }
        return count != null ? count : 1L;
    }

    public boolean isOtpSendingBlockedForTarget(String target) {
        if (target == null || target.isBlank()) return false;
        String normalized = target.trim().toLowerCase();
        String val = redisTemplate.opsForValue().get(OTP_RATE_TARGET_PREFIX + normalized);
        try {
            return val != null && Integer.parseInt(val) >= MAX_OTP_SEND_PER_TARGET;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public long recordOtpSendAttemptForTarget(String target) {
        if (target == null || target.isBlank()) return 1L;
        String normalized = target.trim().toLowerCase();
        String key = OTP_RATE_TARGET_PREFIX + normalized;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, OTP_RATE_WINDOW);
        } else {
            Long ttl = redisTemplate.getExpire(key);
            if (ttl != null && ttl < 0) {
                redisTemplate.expire(key, OTP_RATE_WINDOW);
            }
        }
        return count != null ? count : 1L;
    }

    // ===== RATE LIMIT OTP VERIFICATION FAILURES =====

    public long recordFailedOtpAttempt(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) return 1L;
        String key = OTP_FAIL_PREFIX + sessionToken;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, OTP_FAIL_TTL);
        return count != null ? count : 1L;
    }

    public int getFailedOtpAttempts(String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) return 0;
        String val = redisTemplate.opsForValue().get(OTP_FAIL_PREFIX + sessionToken);
        try {
            return val != null ? Integer.parseInt(val) : 0;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public void clearFailedOtpAttempts(String sessionToken) {
        if (sessionToken != null && !sessionToken.isBlank()) {
            delete(OTP_FAIL_PREFIX + sessionToken);
        }
    }

    public boolean setIfAbsent(String key, String value, Duration ttl) {
        Boolean res = redisTemplate.opsForValue().setIfAbsent(key, value, ttl);
        return Boolean.TRUE.equals(res);
    }

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void setValue(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    // ===== PRIVATE HELPERS =====

    private Duration expiryToTtl(Date expiry) {
        return Duration.ofMillis(expiry.getTime() - System.currentTimeMillis());
    }

    private void save(String key, Object data, Duration ttl) {
        if (ttl.isNegative() || ttl.isZero()) return;
        try {
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data), ttl);
        } catch (Exception e) {
            log.error("Error saving key {}", key, e);
            throw new RuntimeException(e);
        }
    }

    private <T> T read(String key, Class<T> clazz) {
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return null;
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing key {}", key, e);
            return null;
        }
    }

    private void delete(String key) {
        redisTemplate.delete(key);
    }
}
