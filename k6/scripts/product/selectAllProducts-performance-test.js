import http from 'k6/http';
import { check, sleep } from 'k6';
import { SharedArray } from 'k6/data';

// 테스트 설정
export const options = {
  // 다양한 단계로 부하 시나리오 구성
  stages: [
      // 빠른 초기 워밍업
      { duration: '30s', target: 50 },    // 30초 동안 50명으로 빠르게 증가

      // 급격한 트래픽 증가 (ramp up)
      { duration: '1m', target: 300 },    // 1분 동안 300명으로 급격히 증가

      // 더 높은 부하로 빠르게 상승
      { duration: '2m', target: 700 },    // 2분 동안 700명으로 증가

      // 피크 부하 (최대 동시 사용자 1,000명)
      { duration: '2m', target: 1000 },   // 2분 동안 1,000명으로 증가
      { duration: '5m', target: 1000 },   // 5분 동안 1,000명 유지 (피크 부하)

      // 빠른 부하 감소
      { duration: '3m', target: 300 },    // 3분 동안 300명으로 감소
      { duration: '1m', target: 0 }       // 1분 동안 0명으로 감소
    ],
//  stages: [
//    // 빠른 초기 워밍업
//    { duration: '30s', target: 100 },  // 30초 동안 100명으로 빠르게 증가
//
//    // 급격한 트래픽 증가 (ramp up)
//    { duration: '1m', target: 2000 },  // 1분 동안 2,000명으로 급격히 증가
//
//    // 더 높은 부하로 빠르게 상승
//    { duration: '2m', target: 5000 },  // 2분 동안 5,000명으로 증가
//
//    // 피크 부하 (최대 동시 사용자 10,000명)
//    { duration: '2m', target: 10000 }, // 2분 동안 10,000명으로 증가
//    { duration: '5m', target: 10000 }, // 5분 동안 10,000명 유지 (피크 부하)
//
//    // 빠른 부하 감소
//    { duration: '3m', target: 2000 },  // 3분 동안 2,000명으로 감소
//    { duration: '1m', target: 0 }      // 1분 동안 0명으로 감소
//  ],

  thresholds: {
    http_req_duration: ['p(95)<500'], // 95% 요청이 500ms 이내 응답
    http_req_failed: ['rate<0.1'],    // 10% 이하 실패율
  },
};

// 가상 사용자들이 요청할 페이지 크기와 페이지 번호 조합
const pageSizes = [10, 20, 50, 100];
const maxPages = 10; // 최대 페이지 수

// 사용자 행동 시뮬레이션 함수
export default function() {
  // 1. 기본 URL 설정
  const baseUrl = 'http://localhost:8080';

  // 2. 무작위로 페이지 크기 선택
  const randomSizeIndex = Math.floor(Math.random() * pageSizes.length);
  const pageSize = pageSizes[randomSizeIndex];

  // 3. 무작위로 페이지 번호 선택 (0부터 시작)
  const randomPage = Math.floor(Math.random() * maxPages);

  // 4. 요청 URL 생성
  const url = `${baseUrl}/api/products?page=${randomPage}&size=${pageSize}`;

  // 5. 요청 헤더 설정
  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  // 6. GET 요청 실행
  const response = http.get(url, params);

  // 7. 응답 검증
  check(response, {
    'is status 200': (r) => r.status === 200,
    'has products': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body.data && body.data.content && body.data.content.length > 0;
      } catch (e) {
        return false;
      }
    },
    'response time < 200ms': (r) => r.timings.duration < 200,
  });

  // 8. 사용자 행동 시뮬레이션 - 상품 목록 조회 후 다음 행동 전에 잠시 대기
  // 실제 사용자는 페이지를 조회한 후 일정 시간 동안 콘텐츠를 보게 됨
  sleep(Math.random() * 3 + 1); // 1~4초 사이 랜덤하게 대기

  // 9. 몇몇 사용자는 다른 페이지로 이동 (페이지네이션 시뮬레이션)
  if (Math.random() < 0.7) { // 70% 확률로 다음 페이지 조회
    const nextPage = (randomPage + 1) % maxPages;
    const nextUrl = `${baseUrl}?page=${nextPage}&size=${pageSize}`;
    const nextResponse = http.get(nextUrl, params);

    check(nextResponse, {
      'pagination is status 200': (r) => r.status === 200,
    });

    sleep(Math.random() * 2 + 1); // 1~3초 사이 랜덤하게 대기
  }
}

// 테스트 결과 생성
export function handleSummary(data) {
  return {
    'summary.html': htmlReport(data),
    'summary.json': JSON.stringify(data),
  };
}