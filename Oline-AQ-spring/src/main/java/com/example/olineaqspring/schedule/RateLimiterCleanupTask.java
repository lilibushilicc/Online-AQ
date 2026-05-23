package com.example.olineaqspring.schedule;

import com.example.olineaqspring.utils.RateLimiter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterCleanupTask {

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        RateLimiter.cleanUp();
    }
}
