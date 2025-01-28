package kr.hhplus.be.server.common.lock;


public interface DistributedLock {
    boolean acquireLock(Long key, long waitTime, long leaseTime);
    void releaseLock(Long key);
}
