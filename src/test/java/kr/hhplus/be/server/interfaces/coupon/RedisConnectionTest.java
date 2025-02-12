package kr.hhplus.be.server.interfaces.coupon;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ZSetOperations;

import java.util.Set;

@Slf4j
@SpringBootTest
class RedisConnectionTest {

    @Autowired
    @Qualifier("redisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Test
    @DisplayName("Redis 연결 및 설정 테스트")
    void checkRedisConnection() {
        try {
            // Redis 연결 정보 출력
            RedisConnectionFactory factory = redisTemplate.getConnectionFactory();
            log.info("Redis connection factory: {}", factory.getClass().getName());

            // RedisTemplate 설정 정보 출력
            log.info("Key serializer: {}", redisTemplate.getKeySerializer().getClass().getName());
            log.info("Value serializer: {}", redisTemplate.getValueSerializer().getClass().getName());
            log.info("Hash key serializer: {}", redisTemplate.getHashKeySerializer().getClass().getName());
            log.info("Hash value serializer: {}", redisTemplate.getHashValueSerializer().getClass().getName());

            // ZSet 작업을 위한 설정 확인
            log.info("ZSet operations: {}", redisTemplate.opsForZSet().getClass().getName());

            // 실제 Redis 서버 연결 테스트
            redisTemplate.getConnectionFactory().getConnection().ping();
            log.info("Redis connection test successful");

            // 실제 ZSet 작업 테스트
            String testKey = "test:zset";
            redisTemplate.opsForZSet().add(testKey, "testValue", 1.0);
            Set<ZSetOperations.TypedTuple<String>> result = redisTemplate.opsForZSet().popMin(testKey, 1);
            log.info("ZSet test result type: {}", result != null ? result.getClass().getName() : "null");

            Assertions.assertTrue(true, "Redis connection and operations are working correctly");
        } catch (Exception e) {
            log.error("Redis connection check failed", e);
            Assertions.fail("Redis connection test failed: " + e.getMessage());
        }
    }
}
