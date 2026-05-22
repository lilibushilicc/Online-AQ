package com.example.olineaqspring.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class LockUtils {

    private static final ConcurrentMap<String, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    public static ReentrantLock getLock(String key) {
        return LOCKS.computeIfAbsent(key, k -> new ReentrantLock());
    }

    public static boolean tryLock(String key, long timeoutMs) {
        try {
            return getLock(key).tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public static void unlock(String key) {
        ReentrantLock lock = LOCKS.get(key);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public static void cleanUp() {
        LOCKS.clear();
    }
}
