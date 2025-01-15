package kr.hhplus.be.server.interfaces.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Slf4j
@Component
public class ApiLoggingInterceptor implements HandlerInterceptor {
    // MDC 키 상수 정의
    private static final String REQUEST_ID = "requestId";
    private static final String METHOD = "method";
    private static final String URI = "uri";
    private static final String CLIENT_IP = "clientIp";
    private static final String USER_AGENT = "userAgent";
    private static final String START_TIME = "startTime";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 시작 시간 기록
        final long startTime = System.currentTimeMillis();

        // MDC에 요청 정보 저장
        MDC.put(REQUEST_ID, UUID.randomUUID().toString());
        MDC.put(METHOD, request.getMethod());
        MDC.put(URI, request.getRequestURI());
        MDC.put(CLIENT_IP, getClientIp(request));
        MDC.put(USER_AGENT, request.getHeader("User-Agent"));
        MDC.put(START_TIME, String.valueOf(startTime));

        // 요청 시작 로깅
        log.info("API Request Started");

        // 디버그 레벨에서 헤더 정보 로깅
        if (log.isDebugEnabled()) {
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> {
                String headerValue = request.getHeader(headerName);
                // 민감한 헤더 값 마스킹 처리
                if (headerName.toLowerCase().contains("authorization")) {
                    headerValue = "********";
                }
                log.debug("Header -> {}: {}", headerName, headerValue);
            });
        }

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        // 처리 시간 계산
        long duration = System.currentTimeMillis() - Long.parseLong(MDC.get(START_TIME));

        // 응답 정보 로깅
        log.info("API Response -> Status: {} | Duration: {} ms",
                response.getStatus(),
                duration
        );
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        try {
            if (ex != null) {
                log.error("API Error Occurred", ex);
            }
            log.info("API Request Completed");
        } finally {
            // MDC 클리어
            MDC.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
