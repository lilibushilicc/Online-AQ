package com.example.olineaqspring.config;

import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {
    private final JwtUtils jwtUtils;

    public JwtInterceptor(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("未登录或 Token 缺失");
        }
        try {
            Claims claims = jwtUtils.parseToken(authorization.substring(7));
            request.setAttribute("userId", claims.get("userId", Integer.class));
            request.setAttribute("role", claims.get("role", String.class));
        } catch (Exception e) {
            throw new UnauthorizedException("登录已过期或 Token 无效，请重新登录");
        }
        return true;
    }
}
