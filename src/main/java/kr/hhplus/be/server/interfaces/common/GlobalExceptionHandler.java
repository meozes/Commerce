package kr.hhplus.be.server.interfaces.common;

import jakarta.persistence.EntityNotFoundException;
import kr.hhplus.be.server.domain.balance.exception.ChargeBalanceException;
import kr.hhplus.be.server.domain.payment.exception.InsufficientBalanceException;
import kr.hhplus.be.server.domain.product.exception.InsufficientStockException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(EntityNotFoundException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.NOT_FOUND, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

//    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
//    public ResponseEntity<ApiResponse<Void>> handleInvalidDataAccessApiUsageException(InvalidDataAccessApiUsageException e) {
//        ApiResponse<Void> response = ApiResponse.of(HttpStatus.BAD_REQUEST, e.getMessage(), null);
//        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
//    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientStockException(InsufficientStockException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.CONFLICT, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(ChargeBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleChargeBalanceException(ChargeBalanceException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.CONFLICT, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiResponse<Void>> handleInsufficientBalanceException(InsufficientBalanceException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.CONFLICT, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalStateException(IllegalStateException e) {
        ApiResponse<Void> response = ApiResponse.of(HttpStatus.CONFLICT, e.getMessage(), null);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }


}
