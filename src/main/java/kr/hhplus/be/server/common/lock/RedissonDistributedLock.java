package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {
    private final RedissonClient redissonClient;

    @Override
    public boolean acquireLock(Long key, long waitTime, long leaseTime) {
        RLock lock = redissonClient.getLock(key.toString());
        try {
            return lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.info("{} - 락 획득 실패", key);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void releaseLock(Long key) {
        RLock lock = redissonClient.getLock(key.toString());
        if (lock.isHeldByCurrentThread()) {
            log.info("{} - 락 해제", key);
            lock.unlock();
        }
    }
}
