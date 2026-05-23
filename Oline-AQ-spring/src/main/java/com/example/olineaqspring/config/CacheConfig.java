package com.example.olineaqspring.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cm = new CaffeineCacheManager();
        cm.setCaffeine(Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(30, TimeUnit.SECONDS));
        cm.setCacheNames(java.util.List.of("config", "exam", "announcements"));
        return cm;
    }
}
