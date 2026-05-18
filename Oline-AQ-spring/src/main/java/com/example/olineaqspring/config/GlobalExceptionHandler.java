package com.example.olineaqspring.config;

import com.example.olineaqspring.exception.UnauthorizedException;
import com.example.olineaqspring.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ApiResponse<Void> handleUnauthorizedException(UnauthorizedException exception) {
        // 使用 code=400（业务错误），但 HTTP 状态码用 200，让前端能读取到业务错误消息
        return new ApiResponse<>(400, exception.getMessage(), null);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(RuntimeException.class)
    public ApiResponse<Void> handleRuntimeException(RuntimeException exception) {
        return ApiResponse.fail(exception.getMessage());
    }
}
