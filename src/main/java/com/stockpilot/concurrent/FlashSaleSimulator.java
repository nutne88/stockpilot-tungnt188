package com.stockpilot.concurrent;

import com.stockpilot.exception.InsufficientStockException;
import com.stockpilot.repository.ProductRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class FlashSaleSimulator {

    private final ProductRepository productRepository;

    public FlashSaleSimulator(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Result runUnsafe(Long productId, int qtyPerOrder, int numberOfOrders, long simulatedDelayMillis) {
        return run(productId, qtyPerOrder, numberOfOrders, simulatedDelayMillis, false);
    }

    public Result runSafe(Long productId, int qtyPerOrder, int numberOfOrders, long simulatedDelayMillis) {
        return run(productId, qtyPerOrder, numberOfOrders, simulatedDelayMillis, true);
    }

    private Result run(Long productId, int qtyPerOrder, int numberOfOrders,
                       long simulatedDelayMillis, boolean useLock) {
        int poolSize = Math.min(numberOfOrders, 20);
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        AtomicInteger succeeded = new AtomicInteger();
        List<String> rejections = Collections.synchronizedList(new ArrayList<>());
        ReentrantLock lock = StockLockManager.lockFor(productId);

        List<Callable<Void>> tasks = new ArrayList<>();
        for (int i = 1; i <= numberOfOrders; i++) {
            int orderNumber = i;
            tasks.add(() -> {
                try {
                    if (useLock) {
                        lock.lock();
                        try {
                            productRepository.decrementStockCheckThenAct(productId, qtyPerOrder, simulatedDelayMillis);
                        } finally {
                            lock.unlock();
                        }
                    } else {
                        productRepository.decrementStockCheckThenAct(productId, qtyPerOrder, simulatedDelayMillis);
                    }
                    succeeded.incrementAndGet();
                } catch (InsufficientStockException e) {
                    rejections.add("Order #" + orderNumber + " correctly rejected: " + e.getMessage());
                } catch (Exception e) {
                    rejections.add("Order #" + orderNumber + " failed: " + e.getMessage());
                }
                return null;
            });
        }

        try {
            pool.invokeAll(tasks);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            pool.shutdown();
        }

        return new Result(numberOfOrders, succeeded.get(), rejections);
    }

    public static class Result {
        private final int attempted;
        private final int succeeded;
        private final List<String> rejections;

        Result(int attempted, int succeeded, List<String> rejections) {
            this.attempted = attempted;
            this.succeeded = succeeded;
            this.rejections = rejections;
        }

        public int getAttempted() {
            return attempted;
        }

        public int getSucceeded() {
            return succeeded;
        }

        public List<String> getRejections() {
            return rejections;
        }
    }
}