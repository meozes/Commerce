package kr.hhplus.be.server.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class RedissonDistributedLock implements DistributedLock {
    private final RedissonClient redissonClient;
    private static final String LOCK_PREFIX = "LOCK:";
    private static final long WAIT_TIME = 3000L;
    private static final long LEASE_TIME = 3000L;

    @Transactional(propagation = Propagation.NEVER)
    @Override
    public boolean acquireLock(Long key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        log.info("{} - 락 획득 시도", key);
        try {
            return lock.tryLock(WAIT_TIME, LEASE_TIME, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            log.info("{} - 락 획득 실패", key);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void releaseLock(Long key) {
        RLock lock = redissonClient.getLock(LOCK_PREFIX + key);
        if (lock.isHeldByCurrentThread()) {
            log.info("{} - 락 해제", key);
            lock.unlock();
        }
    }
}
