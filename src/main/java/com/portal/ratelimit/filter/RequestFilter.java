package com.portal.ratelimit.filter;

import com.portal.ratelimit.service.RateLimiter;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestFilter extends OncePerRequestFilter {

    @Autowired
    RateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/v1")) {
            String tenantId = request.getHeader("X-Tenant");
            if (StringUtils.hasText(tenantId)) {
                Bucket bucket = rateLimiter.resolveBucket(tenantId);

                if (bucket.tryConsume(1)) {
                    filterChain.doFilter(request, response);
                } else {
                    sendErrorResponse(response, HttpStatus.TOO_MANY_REQUESTS.value());
                }
            } else {
                sendErrorResponse(response, HttpStatus.FORBIDDEN.value());
            }
        } else {
            filterChain.doFilter(request, response);
        }

    }

    private void sendErrorResponse(HttpServletResponse response, int value) throws IOException {
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setStatus(value);

        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.sendError(value);
    }

}
