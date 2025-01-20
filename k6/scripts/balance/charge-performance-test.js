import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    charge: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },  // 30초 동안 0->10 사용자로 증가
        { duration: '1m', target: 10 },   // 1분 동안 10 사용자 유지
        { duration: '30s', target: 0 },   // 30초 동안 10->0 사용자로 감소
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이내 완료
    http_req_failed: ['rate<0.01'],   // 1% 이하의 실패율
  },
};

const BASE_URL = 'http://localhost:8080';
const USER_ID = 77;
const ORDER_ID = 1;

export default function () {
  // 충전 요청
  const chargePayload = JSON.stringify({
    userId: USER_ID,
    amount: 1000  // JUnit 테스트와 동일한 금액으로 설정
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
    'charge response has correct userId': (r) => r.json('data.userId') === USER_ID,
    'charge response has correct amount': (r) => r.json('data.chargedAmount') === 1000,
  });

  // 잔고 확인 요청
  const balanceResponse = http.get(
    `${BASE_URL}/api/balance/${USER_ID}`,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(balanceResponse, {
    'balance status is 200': (r) => r.status === 200,
    'balance response has correct userId': (r) => r.json('data.userId') === USER_ID
  });

  sleep(0.1);  // 0.1초로 줄여서 더 빈번한 동시 요청 발생
}