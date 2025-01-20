import http from 'k6/http';
import { sleep, check } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트 설정
export const options = {
    scenarios: {
        concurrent_coupon_requests: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 100 },  // 30초 동안 50명까지 증가
                { duration: '1m', target: 100 },   // 1분 동안 50명 유지
                { duration: '30s', target: 0 },   // 30초 동안 0명까지 감소
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95%의 요청이 1초 이내 처리
        http_req_failed: ['rate<0.1'],      // 에러율 10% 미만
    },
};

// 테스트에 사용할 사용자 ID 배열 (5부터 104까지 100명의 사용자)
const users = new SharedArray('users', function() {
    return Array.from({ length: 100 }, (_, i) => i + 5);
});

const COUPON_ID = 1; // 테스트할 쿠폰 ID
const BASE_URL = 'http://localhost:8080'; // API 서버 주소

export default function () {
    // 랜덤하게 사용자 선택
    const userId = users[Math.floor(Math.random() * users.length)];

    // 쿠폰 발급 요청
    const response = http.post(
        `${BASE_URL}/api/coupons/${userId}/${COUPON_ID}`,
        null,
        {
            headers: {
                'Content-Type': 'application/json',
            },
        }
    );

    // 성공 또는 예상된 실패(쿠폰 소진)를 모두 정상 케이스로 처리
        check(response, {
            'is status valid': (r) => r.status === 200 || r.status === 404,
            'transaction time OK': (r) => r.timings.duration < 1000,
        });

    // 요청 간 휴식
    sleep(1);
}

// 테스트 완료 후 실행되는 함수
export function handleSummary(data) {
    console.log('Test Summary:');
    console.log('Total Requests:', data.metrics.iterations.count);
    console.log('Failed Requests:', data.metrics.http_req_failed.count);
    console.log('Average Response Time:', data.metrics.http_req_duration.avg);
    console.log('95th Percentile Response Time:', data.metrics.http_req_duration.p95);
}