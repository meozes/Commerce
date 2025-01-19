package kr.hhplus.be.server.common.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import com.google.common.cache.LoadingCache;
import org.hibernate.sql.exec.ExecutionException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class RateLimitFilter implements Filter {

    // IP당 요청 수를 저장하는 캐시
    private final LoadingCache<String, Integer> requestCountsPerIp;

    // 허용되는 최대 요청 수
    private static final int MAX_REQUESTS_PER_MINUTE = 60;

    // rate limit 설정을 주입받기 위한 값
    @Value("${rate.limit.enabled:true}")  // 기본값은 true
    private boolean rateLimitEnabled;

    public RateLimitFilter() {
        // 캐시 설정: 1분 후 만료
        requestCountsPerIp = CacheBuilder.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(new CacheLoader<>() {
                    @Override
                    public Integer load(String key) {
                        return 0;
                    }
                });
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException, ServletException {

        // rate limit이 비활성화되어 있다면 바로 다음 필터로 진행
        if (!rateLimitEnabled) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 클라이언트 IP 가져오기
        String clientIp = getClientIP(httpRequest);

        // 현재 요청 수 증가
        try {
            int requests = requestCountsPerIp.get(clientIp);
            requests++;
            requestCountsPerIp.put(clientIp, requests);

            // 로그 남기기
            log.debug("IP: {}, Current request count: {}", clientIp, requests);

            // 최대 요청 수 초과 체크
            if (requests > MAX_REQUESTS_PER_MINUTE) {
                log.warn("IP: {} exceeded rate limit", clientIp);
                httpResponse.setStatus(429); // Too Many Requests
                httpResponse.getWriter().write("Too many requests. Please try again later.");
                return;
            }

        } catch (ExecutionException e) {
            log.error("Error occurred while getting request count", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new RuntimeException(e);
        }

        // 제한을 넘지 않으면 다음 필터로 진행
        chain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader != null) {
            return xfHeader.split(",")[0];
        }
        return request.getRemoteAddr();
    }
}
