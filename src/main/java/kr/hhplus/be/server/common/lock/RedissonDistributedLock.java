package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "LOCK:";
    private static final long WAIT_TIME = 3000L;
    private static final long LEASE_TIME = 3000L;

    @Override
    public boolean acquireLock(Long key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        try {
            return lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void releaseLock(Long key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
