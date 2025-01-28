import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트용 사용자 ID 배열
const userIds = new SharedArray('users', function() {
    const users = [];
    for (let i = 1; i <= 50; i++) {
        users.push(i);
    }
    return users;
});

// setup에서 주문 데이터 조회
export function setup() {
    const orders = [];
    console.log('Starting setup - collecting order data');
    for (const userId of userIds) {
        console.log(`Fetching order for userId: ${userId}`);
        const response = http.get(`http://localhost:8080/api/orders/${userId}`);
        console.log(`Response status for userId ${userId}: ${response.status}`);

        if (response.status === 200) {
            const responseBody = JSON.parse(response.body);
            const order = responseBody.data;
            console.log(`Received order data for userId ${userId}:`, order);

            if (order && order.orderId) {  // null check 추가
                orders.push({
                    userId: order.userId,
                    orderId: order.orderId,
                    amount: order.finalAmount
                });
                console.log(`Added order to test data: userId=${order.userId}, orderId=${order.id}, amount=${order.finalAmount}`);
            }
        }
    }
    console.log(`Setup complete. Collected ${orders.length} orders for testing`);
    return { orders };
}

export const options = {
    scenarios: {
        restore_stock: {
            executor: 'constant-arrival-rate',
            rate: 20,
            timeUnit: '1s',
            duration: '30s',
            preAllocatedVUs: 50,
            maxVUs: 100,
        },
    },
    thresholds: {
        http_req_failed: ['rate<0.01'],
        http_req_duration: ['p(95)<1000'],
    },
};

export default function(data) {

    // 실행 전 데이터 확인
    if (!data.orders || data.orders.length === 0) {
        console.error('No order data available for testing');
        return;
    }

    // setup에서 가져온 주문 데이터 중 랜덤 선택
    const orderInfo = data.orders[Math.floor(Math.random() * data.orders.length)];

    // 요청 전 데이터 확인
    console.log('Selected order for test:', orderInfo);

    const payload = {
        userId: orderInfo.userId,
        orderId: orderInfo.orderId,
        amount: orderInfo.amount
    };

    // 요청 전 payload 확인
    console.log('Sending payload:', JSON.stringify(payload));

    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const response = http.post(
        'http://localhost:8080/api/payments',
        JSON.stringify(payload),
        params
    );

    // 응답 확인
    console.log(`Response status: ${response.status}, body: ${response.body}`);

    check(response, {
        'is status 200': (r) => r.status === 200,
        'transaction time < 1000ms': (r) => r.timings.duration < 1000,
    });

    sleep(1);
}