package kr.hhplus.be.server.domain.balance.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIdValidator {

    public void validate(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("유저 ID는 필수입니다.");
        }

        if (userId < 0) {
            throw new IllegalArgumentException("유효하지 않은 유저 ID 입니다.");
        }
    }
}
