package com.stockpilot.concurrent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public final class StockLockManager {

    private static final Map<Long, ReentrantLock> LOCKS = new ConcurrentHashMap<>();

    private StockLockManager() {
    }

    public static ReentrantLock lockFor(Long productId) {
        return LOCKS.computeIfAbsent(productId, id -> new ReentrantLock());
    }
}