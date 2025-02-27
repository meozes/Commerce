import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 1000명의 사용자 ID 배열 생성 (1부터 1000까지)
const users = new SharedArray('users', function() {
    return Array.from({ length: 1000 }, (_, i) => i + 1);
});

export const options = {
  scenarios: {
    charge: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 100 },   // 30초 동안 0->100 사용자로 증가
        { duration: '1m', target: 100 },    // 1분 동안 100 사용자 유지
        { duration: '30s', target: 0 },     // 30초 동안 100->0 사용자로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내 완료
    http_req_failed: ['rate<0.01'],   // 1% 이하의 실패율
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 각 VU(Virtual User)는 1000명의 사용자 중 무작위로 한 명을 선택
  const userId = users[Math.floor(Math.random() * users.length)];

  // 충전 요청
  const chargePayload = JSON.stringify({
    userId: userId,
    amount: 1000
  });

  const chargeResponse = http.post(
    `${BASE_URL}/api/balance/charge`,
    chargePayload,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(chargeResponse, {
    'charge status is 200': (r) => r.status === 200,
    'charge response has correct userId': (r) => r.json('data.userId') === userId,
    'charge response has correct amount': (r) => r.json('data.chargedAmount') === 1000,
  });

  // 잔고 확인 요청
  const balanceResponse = http.get(
    `${BASE_URL}/api/balance/${userId}`,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(balanceResponse, {
    'balance status is 200': (r) => r.status === 200,
    'balance response has correct userId': (r) => r.json('data.userId') === userId
  });

  sleep(0.1);
}