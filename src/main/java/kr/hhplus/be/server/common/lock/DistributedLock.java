package kr.hhplus.be.server.common.lock;


public interface DistributedLock {
    boolean acquireLock(Long key);
    void releaseLock(Long key);
}
