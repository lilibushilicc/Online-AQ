package com.example.olineaqspring.aspect;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.olineaqspring.annotation.Idempotent;
import com.example.olineaqspring.entity.IdempotentRecord;
import com.example.olineaqspring.mapper.IdempotentRecordMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class IdempotentAspect {

    private final IdempotentRecordMapper idempotentRecordMapper;

    @Around("@annotation(idempotent)")
    public Object around(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String idempotentKey = request.getHeader("Idempotent-Key");
        if (idempotentKey == null || idempotentKey.isBlank()) {
            return joinPoint.proceed();
        }

        String fullKey = idempotent.prefix() + ":" + idempotentKey;

        IdempotentRecord existing = idempotentRecordMapper.selectById(fullKey);
        if (existing != null) {
            throw new RuntimeException("请求已处理，请勿重复提交");
        }

        try {
            Object result = joinPoint.proceed();

            IdempotentRecord record = new IdempotentRecord();
            record.setIdempotentKey(fullKey);
            record.setResultType(joinPoint.getSignature().toShortString());
            record.setResultJson(result != null ? result.toString() : null);
            record.setExpireTime(LocalDateTime.now().plusSeconds(idempotent.ttlSeconds()));
            try {
                idempotentRecordMapper.insert(record);
            } catch (Exception ignored) {
            }

            return result;
        } catch (Exception e) {
            throw e;
        }
    }
}
