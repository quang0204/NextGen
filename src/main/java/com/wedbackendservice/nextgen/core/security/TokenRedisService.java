package com.wedbackendservice.nextgen.core.security;

import com.wedbackendservice.nextgen.core.dto.session.OtpSessionData;
import com.wedbackendservice.nextgen.core.dto.session.ProfileUpdateSessionData;
import com.wedbackendservice.nextgen.core.dto.session.RegisterSessionData;
import com.wedbackendservice.nextgen.core.dto.session.ResetPasswordSessionData;
import com.wedbackendservice.nextgen.core.dto.session.TokenPair;
import com.wedbackendservice.nextgen.core.dto.session.UploadSessionData;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.function.LongFunction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenRedisService {

  private static final String SESSION_PREFIX = "token:session:";
  private static final String TOKEN_BLACKLIST_JTI_PREFIX = "token:blacklist:jti:";
  private static final String TOKEN_VERSION_PREFIX = "token:version:";
  private static final String OTP_PREFIX = "otp:session:";
  private static final String LOGIN_FAIL_PREFIX = "login:fail:";
  private static final String REG_PREFIX = "reg_token_";
  private static final String RESET_PREFIX = "reset_token_";
  private static final String RESET_USER_PREFIX = "reset_user_";
  private static final String PROFILE_UPDATE_PREFIX = "profile_update_session:";
  private static final String OTP_RATE_IP_PREFIX = "otp:rate:ip:";
  private static final String OTP_RATE_TARGET_PREFIX = "otp:rate:target:";
  private static final String OTP_FAIL_PREFIX = "otp:fail:";
  private static final String STORAGE_UPLOAD_PREFIX = "storage:upload:";
  public static final String STORAGE_PENDING_ZSET = "storage:pending:zset";
  private static final String LOCK_REG_PREFIX = "lock:register:";
  private static final String LOCK_REG_IDENTITY_PREFIX = "lock:reg:identity:";
  private static final int MAX_FAILED_ATTEMPTS = 5;
  public static final int MAX_OTP_SEND_PER_IP = 10;
  public static final int MAX_OTP_SEND_PER_TARGET = 5;
  public static final int MAX_OTP_FAIL_ATTEMPTS = 5;
  private static final Duration FAIL_LOCK_DURATION = Duration.ofMinutes(15);
  private static final Duration OTP_RATE_WINDOW = Duration.ofMinutes(15);
  private static final Duration OTP_FAIL_TTL = Duration.ofSeconds(120);
  private static final Duration TOKEN_VERSION_CACHE_TTL = Duration.ofDays(7);

  private final RedisTemplate<String, String> redisTemplate;
  private final ObjectMapper objectMapper;

  // ===== JWT SESSION / JTI BLACKLIST / TOKEN VERSION =====

  public void saveToken(
      Long userId, String sessionId, String accessToken, String refreshToken, Date refreshExpiry) {
    save(
        sessionKey(userId, sessionId),
        new TokenPair(accessToken, refreshToken),
        expiryToTtl(refreshExpiry));
  }

  public TokenPair getTokenPair(Long userId, String sessionId) {
    return read(sessionKey(userId, sessionId), TokenPair.class);
  }

  public void deleteToken(Long userId, String sessionId) {
    delete(sessionKey(userId, sessionId));
  }

  public void deleteToken(Long userId) {
    delete(SESSION_PREFIX + userId);
  }

  public void blacklistJwt(String jti, Date expiry) {
    if (jti == null || jti.isBlank() || expiry == null) return;
    Duration ttl = expiryToTtl(expiry);
    if (ttl.isNegative() || ttl.isZero()) return;
    redisTemplate.opsForValue().set(TOKEN_BLACKLIST_JTI_PREFIX + jti, "1", ttl);
  }

  public boolean isJwtBlacklisted(String jti) {
    if (jti == null || jti.isBlank()) return true;
    return redisTemplate.opsForValue().get(TOKEN_BLACKLIST_JTI_PREFIX + jti) != null;
  }

  public boolean isTokenVersionValid(
      Long userId, long tokenVersion, LongFunction<Optional<Long>> dbLoader) {
    if (userId == null || tokenVersion < 0) return false;
    String key = TOKEN_VERSION_PREFIX + userId;
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
      try {
        return Long.parseLong(cached) == tokenVersion;
      } catch (NumberFormatException ignored) {
        delete(key);
      }
    }
    Optional<Long> fromDb = dbLoader.apply(userId);
    if (fromDb.isEmpty()) return false;
    long dbVersion = fromDb.get();
    saveTokenVersion(userId, dbVersion);
    return dbVersion == tokenVersion;
  }

  public void revokeAllSessions(Long userId, long newTokenVersion) {
    if (userId == null) return;
    deleteToken(userId);
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
      // Defer Redis write until DB transaction commits so rollbacks never leave stale versions.
      TransactionSynchronizationManager.registerSynchronization(
          new TransactionSynchronization() {
            @Override
            public void afterCommit() {
              saveTokenVersion(userId, newTokenVersion);
            }
          });
      return;
    }
    saveTokenVersion(userId, newTokenVersion);
  }

  public void saveTokenVersion(Long userId, long version) {
    if (userId == null) return;
    redisTemplate
        .opsForValue()
        .set(TOKEN_VERSION_PREFIX + userId, String.valueOf(version), TOKEN_VERSION_CACHE_TTL);
  }

  private static String sessionKey(Long userId, String sessionId) {
    return (sessionId == null || sessionId.isBlank())
        ? SESSION_PREFIX + userId
        : SESSION_PREFIX + userId + ":" + sessionId;
  }

  // ===== OTP / REGISTER / RESET SESSION =====

  public void saveOtpSession(String token, OtpSessionData data, long ttlSeconds) {
    save(OTP_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
  }

  public OtpSessionData getOtpSession(String token) {
    return read(OTP_PREFIX + token, OtpSessionData.class);
  }

  public void deleteOtpSession(String token) {
    delete(OTP_PREFIX + token);
  }

  public void saveRegisterSession(String token, RegisterSessionData data, long ttlSeconds) {
    save(REG_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
  }

  public RegisterSessionData getRegisterSession(String token) {
    return read(REG_PREFIX + token, RegisterSessionData.class);
  }

  public void deleteRegisterSession(String token) {
    delete(REG_PREFIX + token);
  }

  // ===== ATOMIC DISTRIBUTED LOCKS =====

  public boolean acquireLock(String lockKey, Duration ttl) {
    Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", ttl);
    return Boolean.TRUE.equals(success);
  }

  public void releaseLock(String lockKey) {
    redisTemplate.delete(lockKey);
  }

  public boolean acquireRegisterLock(String registrationToken, Duration ttl) {
    return acquireLock(LOCK_REG_PREFIX + registrationToken, ttl);
  }

  public void releaseRegisterLock(String registrationToken) {
    releaseLock(LOCK_REG_PREFIX + registrationToken);
  }

  public boolean acquireIdentityLock(String identity, Duration ttl) {
    if (identity == null || identity.isBlank()) return true;
    return acquireLock(LOCK_REG_IDENTITY_PREFIX + identity.toLowerCase().trim(), ttl);
  }

  public void releaseIdentityLock(String identity) {
    if (identity == null || identity.isBlank()) return;
    releaseLock(LOCK_REG_IDENTITY_PREFIX + identity.toLowerCase().trim());
  }

  public void saveResetPasswordSession(
      String token, ResetPasswordSessionData data, long ttlSeconds) {
    Duration ttl = Duration.ofSeconds(ttlSeconds);
    if (token == null || token.isBlank() || ttl.isNegative() || ttl.isZero()) return;
    save(RESET_PREFIX + token, data, ttl);

    // Keep at most one active resetToken per user so superseded tokens are immediately rejected.
    if (data != null && data.getUserId() != null) {
      String userKey = RESET_USER_PREFIX + data.getUserId();
      String previousToken = redisTemplate.opsForValue().getAndSet(userKey, token);
      redisTemplate.expire(userKey, ttl);
      if (previousToken != null && !previousToken.isBlank() && !previousToken.equals(token)) {
        delete(RESET_PREFIX + previousToken);
        clearFailedOtpAttempts(previousToken);
      }
    }
  }

  public ResetPasswordSessionData getResetPasswordSession(String token) {
    if (token == null || token.isBlank()) return null;
    return read(RESET_PREFIX + token, ResetPasswordSessionData.class);
  }

  public void deleteResetPasswordSession(String token) {
    if (token == null || token.isBlank()) return;
    ResetPasswordSessionData existing = getResetPasswordSession(token);
    delete(RESET_PREFIX + token);
    if (existing != null && existing.getUserId() != null) {
      String userKey = RESET_USER_PREFIX + existing.getUserId();
      if (token.equals(redisTemplate.opsForValue().get(userKey))) {
        delete(userKey);
      }
    }
  }

  public void saveProfileUpdateSession(
      String token, ProfileUpdateSessionData data, long ttlSeconds) {
    save(PROFILE_UPDATE_PREFIX + token, data, Duration.ofSeconds(ttlSeconds));
  }

  public ProfileUpdateSessionData getProfileUpdateSession(String token) {
    return read(PROFILE_UPDATE_PREFIX + token, ProfileUpdateSessionData.class);
  }

  public void deleteProfileUpdateSession(String token) {
    delete(PROFILE_UPDATE_PREFIX + token);
  }

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

  public void deleteKey(String key) {
    if (key != null && !key.isBlank()) {
      delete(key);
    }
  }

  // ===== STORAGE UPLOAD SESSION & PENDING ZSET =====

  public void saveUploadSession(String uploadId, UploadSessionData data, Duration ttl) {
    save(STORAGE_UPLOAD_PREFIX + uploadId, data, ttl);
  }

  public UploadSessionData getUploadSession(String uploadId) {
    if (uploadId == null || uploadId.isBlank()) return null;
    return read(STORAGE_UPLOAD_PREFIX + uploadId, UploadSessionData.class);
  }

  public void deleteUploadSession(String uploadId) {
    if (uploadId != null && !uploadId.isBlank()) {
      delete(STORAGE_UPLOAD_PREFIX + uploadId);
    }
  }

  public void addPendingUpload(String uploadId, long expiresAtEpochMillis) {
    if (uploadId != null && !uploadId.isBlank()) {
      redisTemplate.opsForZSet().add(STORAGE_PENDING_ZSET, uploadId, expiresAtEpochMillis);
    }
  }

  public void removePendingUpload(String uploadId) {
    if (uploadId != null && !uploadId.isBlank()) {
      redisTemplate.opsForZSet().remove(STORAGE_PENDING_ZSET, uploadId);
    }
  }

  public Set<String> getExpiredPendingUploadIds(long maxScoreEpochMillis, long count) {
    return redisTemplate
        .opsForZSet()
        .rangeByScore(STORAGE_PENDING_ZSET, 0, maxScoreEpochMillis, 0, count);
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
