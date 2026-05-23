package com.example.olineaqspring.config;

import com.example.olineaqspring.annotation.AdminOnly;
import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
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
            String role = claims.get("role", String.class);
            request.setAttribute("role", role);
            if (handler instanceof HandlerMethod handlerMethod) {
                AdminOnly adminOnly = handlerMethod.getMethodAnnotation(AdminOnly.class);
                if (adminOnly == null) {
                    adminOnly = handlerMethod.getBeanType().getAnnotation(AdminOnly.class);
                }
                if (adminOnly != null && !"admin".equals(role)) {
                    throw new UnauthorizedException(adminOnly.value());
                }
            }
        } catch (Exception e) {
            if (e instanceof UnauthorizedException unauthorizedException) {
                throw unauthorizedException;
            }
            throw new UnauthorizedException("登录已过期或 Token 无效，请重新登录");
        }
        return true;
    }
}
