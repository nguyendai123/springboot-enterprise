package com.enterprise.app.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

    private final CacheManager cacheManager;
    private final RedisTemplate<String, Object> redisTemplate;

    // ── Spring Cache abstraction ──────────────────────────────────

    public <T> T get(String cacheName, String key, Class<T> type) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) return null;
        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper == null) return null;
        return type.cast(wrapper.get());
    }

    public void put(String cacheName, String key, Object value) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) cache.put(key, value);
    }

    public void evict(String cacheName, String key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
            log.debug("Evicted cache={} key={}", cacheName, key);
        }
    }

    public void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
            log.info("Cleared cache={}", cacheName);
        }
    }

    // ── Direct Redis operations ───────────────────────────────────

    public void setWithTtl(String key, Object value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void expire(String key, long seconds) {
        redisTemplate.expire(key, seconds, TimeUnit.SECONDS);
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long incrementBy(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    // ── Distributed lock using Redisson (via RedisTemplate SETNX) ─

    public boolean tryLock(String lockKey, String lockValue, Duration ttl) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent("lock:" + lockKey, lockValue, ttl);
        if (Boolean.TRUE.equals(acquired)) {
            log.debug("Lock acquired: {}", lockKey);
            return true;
        }
        log.debug("Lock not available: {}", lockKey);
        return false;
    }

    public void releaseLock(String lockKey, String lockValue) {
        String key = "lock:" + lockKey;
        Object current = redisTemplate.opsForValue().get(key);
        if (lockValue.equals(current)) {
            redisTemplate.delete(key);
            log.debug("Lock released: {}", lockKey);
        }
    }

    // ── Token blacklist (for JWT logout) ─────────────────────────

    public void blacklistToken(String jti, Duration ttl) {
        redisTemplate.opsForValue().set("blacklist:" + jti, "true", ttl);
    }

    public boolean isTokenBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + jti));
    }

    // ── OTP / Verification codes ──────────────────────────────────

    public void storeOtp(String email, String otp, Duration ttl) {
        redisTemplate.opsForValue().set("otp:" + email, otp, ttl);
    }

    public String getOtp(String email) {
        Object val = redisTemplate.opsForValue().get("otp:" + email);
        return val != null ? val.toString() : null;
    }

    public void deleteOtp(String email) {
        redisTemplate.delete("otp:" + email);
    }

    // ── Session store ─────────────────────────────────────────────

    public void storeSession(String sessionId, Object sessionData, Duration ttl) {
        redisTemplate.opsForValue().set("session:" + sessionId, sessionData, ttl);
    }

    public Object getSession(String sessionId) {
        return redisTemplate.opsForValue().get("session:" + sessionId);
    }

    // ── Pattern-based eviction ────────────────────────────────────

    public void evictByPattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            log.info("Evicted {} keys matching pattern: {}", keys.size(), pattern);
        }
    }
}