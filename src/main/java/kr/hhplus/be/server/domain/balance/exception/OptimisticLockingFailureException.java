package kr.hhplus.be.server.domain.balance.exception;

import org.springframework.orm.ObjectOptimisticLockingFailureException;

public class OptimisticLockingFailureException extends RuntimeException{
    public OptimisticLockingFailureException(String message, ObjectOptimisticLockingFailureException e) {
        super(message);
    }
}
