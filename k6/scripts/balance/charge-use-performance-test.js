import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';
import { SharedArray } from 'k6/data';

const BASE_URL = 'http://localhost:8080';
const USER_ID = 77;
const ORDER_ID = 1;
const CHARGE_AMOUNT = 50000;
const USE_AMOUNT = 5000;

// 커스텀 메트릭 정의
const successfulCharges = new Counter('successful_charges');
const successfulPayments = new Counter('successful_payments');


// 테스트 설정
export const options = {
  scenarios: {
    charge_and_use: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 3 },  // 충전 요청용 VU 3개로 램프업
        { duration: '1m', target: 3 },   // 1분간 부하 유지
        { duration: '30s', target: 0 },  // 램프다운
      ],
      gracefulRampDown: '30s',
      exec: 'charge',
    },
    payment: {
      executor: 'ramping-arrival-rate', // 동시성 테스트를 위해 변경
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 5,
            maxVUs: 10,
            stages: [
              { duration: '10s', target: 5 }, // 짧은 시간 동안 여러 요청을 동시에 보냄
              { duration: '20s', target: 0 },
            ],
            exec: 'payment',
    },
  },
};

// 충전 시나리오
export function charge() {
  const payload = JSON.stringify({
    userId: USER_ID,
    amount: CHARGE_AMOUNT,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = check(http.post(`${BASE_URL}/api/balance/charge`, payload, params), {
    'charge status is 200': (r) => r.status === 200,
    'charge response has correct userId': (r) => r.json('data.userId') === USER_ID,
    'charge response has correct amount': (r) => r.json('data.chargedAmount') === CHARGE_AMOUNT,
  });

  if (response) {
    successfulCharges.add(1);
  }

  sleep(1);
}

// 결제 시나리오
export function payment() {
  const payload = JSON.stringify({
    userId: USER_ID,
    orderId: ORDER_ID,
    amount: USE_AMOUNT,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const response = http.post(`${BASE_URL}/api/payments`, payload, params);

    // 첫 번째 요청은 성공해야 함
    if (response.status === 200) {
      check(response, {
        'payment status is 200': (r) => r.status === 200,
        'payment response has correct orderId': (r) => r.json('data.orderId') === ORDER_ID,
        'payment response has correct userId': (r) => r.json('data.userId') === USER_ID,
        'payment status is COMPLETED': (r) => r.json('data.status') === 'COMPLETED',
      });
      successfulPayments.add(1);
    }
    // 이후 요청들은 적절한 에러를 반환해야 함
    else {
      check(response, {
        'duplicate payment returns error status': (r) => r.status === 400,
        'error message is correct': (r) => r.json('message') === ErrorCode.ORDER_ALREADY_COMPLETED.getMessage(),
      });
    }

  sleep(1);
}

// 테스트 완료 후 잔고 확인을 위한 함수
export function teardown() {
  const response = http.get(`${BASE_URL}/api/balance/${USER_ID}`);
  console.log(`Final balance check - Status: ${response.status}, Balance: ${response.json('data.balance')}`);
}