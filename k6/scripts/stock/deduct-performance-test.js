import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트 설정
export const options = {
    scenarios: {
        deduct_stock: {
            executor: 'constant-arrival-rate',
            rate: 50,              // 초당 50개의 요청 생성
            timeUnit: '1s',        // 1초 단위로
            duration: '30s',       // 30초 동안 실행
            preAllocatedVUs: 50,   // 미리 할당할 VU (Virtual User) 수
            maxVUs: 100,          // 최대 VU 수
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],    // 요청 실패율 1% 미만
        http_req_duration: ['p(95)<1000'],  // 95% 요청이 1초 이내 완료
    },
};

// 테스트용 사용자 ID 배열
const userIds = new SharedArray('users', function() {
    const users = [];
    for (let i = 1; i <= 50; i++) {
        users.push(i);
    }
    return users;
});

export default function() {
    const baseUrl = 'http://localhost:8080';  // API 서버 주소
    const userId = userIds[Math.floor(Math.random() * userIds.length)];

    // 주문 요청 데이터
    const payload = {
        userId: userId,
        items: [
            {
                productId: 1,        // 테스트용 상품 ID
                productName: "쿠키",
                quantity: 1,         // 각 주문당 2개씩 주문
                price: 10000
            }
        ]
    };

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    // 주문 API 호출
    const response = http.post(
        `${baseUrl}/api/orders`,
        JSON.stringify(payload),
        params
    );

    // 응답 검증
    check(response, {
        'is status 200': (r) => r.status === 200,
        'transaction time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}