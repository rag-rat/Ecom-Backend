package com.ecom.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";


    public void blacklistToken(String jti, long remainingTtlMs) {
        String key = BLACKLIST_PREFIX + jti;


        redisTemplate.opsForValue().set(
                key,
                "BLACKLISTED",
                remainingTtlMs,
                TimeUnit.MILLISECONDS
        );
        log.info("Token blacklisted. JTI: {}, TTL: {}ms", jti, remainingTtlMs);
    }


    public boolean isBlacklisted(String jti) {
        String key = BLACKLIST_PREFIX + jti;

        Boolean exists = redisTemplate.hasKey(key);
        return Boolean.TRUE.equals(exists);
    }

}
