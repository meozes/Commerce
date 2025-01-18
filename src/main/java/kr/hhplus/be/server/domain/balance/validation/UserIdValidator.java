package kr.hhplus.be.server.domain.balance.validation;

import kr.hhplus.be.server.interfaces.common.type.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIdValidator {

    public void validate(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException(ErrorCode.USER_ID_REQUIRED.getMessage());
        }

        if (userId < 0) {
            throw new IllegalArgumentException(ErrorCode.INVALID_USER_ID.getMessage());
        }
    }
}
