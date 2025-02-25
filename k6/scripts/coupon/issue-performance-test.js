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
                        { duration: '30s', target: 50 },    // 30초 동안 워밍업 (50명까지 증가)
                        { duration: '1m', target: 250 },    // 1분 동안 피크 트래픽으로 증가 (250명)
                        { duration: '2m', target: 250 },    // 2분 동안 피크 트래픽 유지
                        { duration: '1m', target: 100 },    // 1분 동안 중간 부하로 감소
                        { duration: '30s', target: 0 },     // 30초 동안 테스트 종료
                    ],
                    gracefulRampDown: '30s',
                },
    },
    thresholds: {
        http_req_duration: ['p(95)<2000'], // 95%의 요청이 1초 이내 처리
        http_req_failed: ['rate<0.1'],      // 에러율 10% 미만
    },
};

// 테스트에 사용할 사용자 ID 배열 (1부터 1000까지 1000명의 사용자)
const users = new SharedArray('users', function() {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

const COUPON_ID = 1; // 테스트할 쿠폰 ID
const BASE_URL = 'http://localhost:8080'; // API 서버 주소

export default function () {
    // 1000명의 사용자 중에서 완전히 랜덤하게 선택 (이미 사용된 사용자도 재사용 가능)
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

    // 응답 확인
    // 성공(200), 쿠폰 소진(404), 중복 발급 시도(409) 모두 정상 케이스로 간주
        check(response, {
            'is status valid': (r) => r.status === 200 || r.status === 404 || r.status === 409,
            'transaction time OK': (r) => r.timings.duration < 2000,
        });

    // 요청 간 휴식
    sleep(Math.random() * 0.1);
}

// 테스트 완료 후 실행되는 함수
export function handleSummary(data) {
    console.log('==== 쿠폰 부하 테스트 결과 요약 ====');
        console.log('총 요청 수:', data.metrics.iterations.count);
        console.log('실패한 요청 수:', data.metrics.http_req_failed.count);
        console.log('평균 응답 시간 (ms):', data.metrics.http_req_duration.avg.toFixed(2));
        console.log('95th 백분위 응답 시간 (ms):', data.metrics.http_req_duration.p(95).toFixed(2));
        console.log('최대 응답 시간 (ms):', data.metrics.http_req_duration.max.toFixed(2));
        console.log('RPS(초당 요청 수):', (data.metrics.iterations.count / (data.state.testRunDurationMs / 1000)).toFixed(2));

        // JSON 형식 결과 반환 (결과 파일로 저장하기 위함)
        return {
            'summary.json': JSON.stringify(data, null, 4),
            'summary.html': generateHtmlReport(data),
        };
}