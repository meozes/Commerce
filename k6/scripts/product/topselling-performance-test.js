import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';
import { Trend, Counter, Rate } from 'k6/metrics';

// 사용자 정의 메트릭 설정
const trends = {
  TopProductsTrend: new Trend('top_products_api_duration'),
};

const counters = {
  TopProductsErrors: new Counter('top_products_errors'),
};

const rates = {
  TopProductsSuccessRate: new Rate('top_products_success_rate'),
};

export const options = {
  // 시나리오 설정
  scenarios: {
//    // 첫 번째 시나리오: 평소 트래픽 (20-30 RPS)
//    normal_traffic: {
//      executor: 'ramping-vus',
//      startVUs: 5,
//      stages: [
//        { duration: '1m', target: 20 }, // 1분간 서서히 트래픽 증가 (약 20 RPS)
//        { duration: '3m', target: 20 }, // 3분간 유지
//      ],
//      gracefulRampDown: '30s',
//    },

    // 두 번째 시나리오: 트래픽 피크 시간 (100-250 RPS)
    peak_traffic: {
      executor: 'ramping-vus',
      startVUs: 0,
      startTime: '4m',
      stages: [
        { duration: '30s', target: 100 }, // 30초 동안 급격히 트래픽 증가 (약 100 RPS)
        { duration: '2m', target: 250 }, // 2분 동안 250 RPS로 증가
        { duration: '5m', target: 250 }, // 5분 동안 피크 트래픽 유지
        { duration: '2m', target: 50 }, // 2분 동안 서서히 트래픽 감소
      ],
      gracefulRampDown: '30s',
    }
//
//    // 세 번째 시나리오: 안정화 단계
//    stabilization: {
//      executor: 'ramping-vus',
//      startVUs: 50,
//      startTime: '13m30s',
//      stages: [
//        { duration: '3m', target: 30 }, // 3분 동안 점진적 감소
//        { duration: '2m', target: 20 }, // 2분 동안 정상 수준으로 회귀
//      ],
//      gracefulRampDown: '30s',
//    },
  },
  thresholds: {
    'top_products_api_duration': ['avg < 300', 'p(95) < 300', 'p(99) < 500'],
    'top_products_success_rate': ['rate > 0.98'], // 98% 이상 성공률
    'http_req_duration': ['p(95) < 300'], // 전체 요청의 95%가 300ms 이내 완료
  },
};

// 기본 설정값
const BASE_URL = 'http://localhost:8080'; // 서버 URL 변경 필요
const API_ENDPOINT = '/api/products/top';

// 요청 헤더
const headers = {
  'Content-Type': 'application/json'
};

// 테스트 함수
export default function() {
  // 현재 시나리오 이름 가져오기
  const scenario = __ENV.SCENARIO || 'unknown';

  // 타임스탬프
  const timestamp = new Date().toISOString();

  // 인기 상품 조회 API 호출
  const response = sendTopProductsRequest();

  // 결과 검증 및 메트릭 기록
  processResponse(response, scenario, timestamp);

  // 시나리오별 대기 시간 조정
  if (scenario === 'peak_traffic') {
    sleep(0.2); // 피크 트래픽에서는 짧게 대기 (0.2초)
  } else {
    sleep(1); // 일반 상황에서는 1초 대기
  }
}

// 인기 상품 API 요청 함수
function sendTopProductsRequest() {
  const url = `${BASE_URL}${API_ENDPOINT}`;
  const start = new Date();
  const response = http.get(url, { headers });
  const end = new Date();
  const duration = end - start;

  // 요청 지속 시간 기록
  trends.TopProductsTrend.add(duration);

  return response;
}

// 응답 처리 및 검증 함수
function processResponse(response, scenario, timestamp) {
  const checkResult = check(response, {
    'status is 200': (r) => r.status === 200,
    'response body has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body &&
               body.data &&
               Array.isArray(body.data.products) &&
               body.data.products.length > 0;
      } catch (e) {
        return false;
      }
    },
  });

  // 성공 여부 기록
  rates.TopProductsSuccessRate.add(checkResult);

  // 오류 발생 시 카운터 증가
  if (!checkResult) {
    counters.TopProductsErrors.add(1);
    console.error(`Error in ${scenario} at ${timestamp}: Status ${response.status}, Body: ${response.body.substring(0, 200)}`);
  }
}