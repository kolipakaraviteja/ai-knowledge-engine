package com.enterprise.ai.knowledge.assistant.logging;

import com.enterprise.ai.knowledge.assistant.config.CorrelationIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performance tracking utility for timing operations and logging performance metrics.
 * Supports nested timing for complex operations and configurable warning thresholds.
 */
@Slf4j
@Component
public class PerformanceLogger {

    private static final String COMPONENT = "PERFORMANCE";
    private static final String OPERATION = "operation";
    private static final String OPERATION_NAME = "operation_name";
    private static final String DURATION_MS = "duration_ms";
    private static final String THRESHOLD_MS = "threshold_ms";
    private static final String NESTING_LEVEL = "nesting_level";

    private final long performanceWarningThreshold;
    private final ThreadLocal<Stack<TimingContext>> timingStack = ThreadLocal.withInitial(Stack::new);
    private final ConcurrentHashMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();

    public PerformanceLogger(@Value("${app.logging.performance.threshold:1000}") long performanceWarningThreshold) {
        this.performanceWarningThreshold = performanceWarningThreshold;
    }

    /**
     * Start timing an operation
     */
    public TimingContext startTiming(String operationName) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        TimingContext context = new TimingContext(operationName, System.currentTimeMillis(), correlationId);
        timingStack.get().push(context);
        
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "timing_start");
        MDC.put(OPERATION_NAME, operationName);
        MDC.put(NESTING_LEVEL, String.valueOf(timingStack.get().size()));
        
        log.debug("[{}] Timing started: operation={}, nesting_level={}", 
                correlationId, operationName, timingStack.get().size());
        
        clearMdc();
        return context;
    }

    /**
     * Stop timing an operation and log the duration
     */
    public void stopTiming(TimingContext context) {
        if (context == null) {
            log.warn("Attempted to stop null timing context");
            return;
        }

        long duration = System.currentTimeMillis() - context.startTime;
        int nestingLevel = timingStack.get().size();
        
        // Remove from stack if it's the top
        if (!timingStack.get().isEmpty() && timingStack.get().peek() == context) {
            timingStack.get().pop();
        }

        // Update statistics
        updateStats(context.operationName, duration);

        // Log with appropriate level based on threshold
        String correlationId = context.correlationId;
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "timing_complete");
        MDC.put(OPERATION_NAME, context.operationName);
        MDC.put(DURATION_MS, String.valueOf(duration));
        MDC.put(THRESHOLD_MS, String.valueOf(performanceWarningThreshold));
        MDC.put(NESTING_LEVEL, String.valueOf(nestingLevel));

        if (duration > performanceWarningThreshold) {
            log.warn("[{}] SLOW OPERATION: operation={}, duration={}ms, threshold={}ms, nesting_level={}", 
                    correlationId, context.operationName, duration, performanceWarningThreshold, nestingLevel);
        } else {
            log.debug("[{}] Timing completed: operation={}, duration={}ms, nesting_level={}", 
                    correlationId, context.operationName, duration, nestingLevel);
        }

        clearMdc();
    }

    /**
     * Stop timing and log with custom threshold
     */
    public void stopTiming(TimingContext context, long customThreshold) {
        if (context == null) {
            log.warn("Attempted to stop null timing context");
            return;
        }

        long duration = System.currentTimeMillis() - context.startTime;
        int nestingLevel = timingStack.get().size();
        
        // Remove from stack if it's the top
        if (!timingStack.get().isEmpty() && timingStack.get().peek() == context) {
            timingStack.get().pop();
        }

        // Update statistics
        updateStats(context.operationName, duration);

        // Log with appropriate level based on custom threshold
        String correlationId = context.correlationId;
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "timing_complete");
        MDC.put(OPERATION_NAME, context.operationName);
        MDC.put(DURATION_MS, String.valueOf(duration));
        MDC.put(THRESHOLD_MS, String.valueOf(customThreshold));
        MDC.put(NESTING_LEVEL, String.valueOf(nestingLevel));

        if (duration > customThreshold) {
            log.warn("[{}] SLOW OPERATION: operation={}, duration={}ms, threshold={}ms, nesting_level={}", 
                    correlationId, context.operationName, duration, customThreshold, nestingLevel);
        } else {
            log.debug("[{}] Timing completed: operation={}, duration={}ms, nesting_level={}", 
                    correlationId, context.operationName, duration, nestingLevel);
        }

        clearMdc();
    }

    /**
     * Log a performance metric directly
     */
    public void logMetric(String operationName, long durationMs) {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "metric");
        MDC.put(OPERATION_NAME, operationName);
        MDC.put(DURATION_MS, String.valueOf(durationMs));
        MDC.put(THRESHOLD_MS, String.valueOf(performanceWarningThreshold));

        if (durationMs > performanceWarningThreshold) {
            log.warn("[{}] PERFORMANCE METRIC: operation={}, duration={}ms, threshold={}ms", 
                    correlationId, operationName, durationMs, performanceWarningThreshold);
        } else {
            log.debug("[{}] PERFORMANCE METRIC: operation={}, duration={}ms", 
                    correlationId, operationName, durationMs);
        }

        // Update statistics
        updateStats(operationName, durationMs);

        clearMdc();
    }

    /**
     * Get statistics for a specific operation
     */
    public OperationStats getStats(String operationName) {
        return operationStats.getOrDefault(operationName, new OperationStats(operationName));
    }

    /**
     * Log all accumulated statistics
     */
    public void logStatistics() {
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "statistics");

        log.info("[{}] Performance Statistics: {}", correlationId, operationStats);

        clearMdc();
    }

    /**
     * Reset all statistics
     */
    public void resetStatistics() {
        operationStats.clear();
        String correlationId = CorrelationIdUtil.getCorrelationId();
        MDC.put("component", COMPONENT);
        MDC.put(OPERATION, "statistics_reset");

        log.info("[{}] Performance statistics reset", correlationId);

        clearMdc();
    }

    /**
     * Update operation statistics
     */
    private void updateStats(String operationName, long duration) {
        operationStats.compute(operationName, (key, stats) -> {
            if (stats == null) {
                stats = new OperationStats(key);
            }
            stats.recordExecution(duration);
            return stats;
        });
    }

    /**
     * Clear MDC after logging operations
     */
    private void clearMdc() {
        MDC.remove("component");
        MDC.remove(OPERATION);
        MDC.remove(OPERATION_NAME);
        MDC.remove(DURATION_MS);
        MDC.remove(THRESHOLD_MS);
        MDC.remove(NESTING_LEVEL);
    }

    /**
     * Timing context for tracking operation duration
     */
    public static class TimingContext {
        private final String operationName;
        private final long startTime;
        private final String correlationId;

        public TimingContext(String operationName, long startTime, String correlationId) {
            this.operationName = operationName;
            this.startTime = startTime;
            this.correlationId = correlationId;
        }

        public String getOperationName() {
            return operationName;
        }

        public long getStartTime() {
            return startTime;
        }

        public String getCorrelationId() {
            return correlationId;
        }
    }

    /**
     * Statistics for tracking operation performance over time
     */
    public static class OperationStats {
        private final String operationName;
        private long executionCount;
        private long totalDuration;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = Long.MIN_VALUE;
        private long slowCount; // Count of operations exceeding threshold

        public OperationStats(String operationName) {
            this.operationName = operationName;
        }

        public void recordExecution(long duration) {
            executionCount++;
            totalDuration += duration;
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);
        }

        public void recordSlowExecution(long duration) {
            slowCount++;
        }

        public double getAverageDuration() {
            return executionCount > 0 ? (double) totalDuration / executionCount : 0;
        }

        public String getOperationName() {
            return operationName;
        }

        public long getExecutionCount() {
            return executionCount;
        }

        public long getTotalDuration() {
            return totalDuration;
        }

        public long getMinDuration() {
            return minDuration;
        }

        public long getMaxDuration() {
            return maxDuration;
        }

        public long getSlowCount() {
            return slowCount;
        }

        @Override
        public String toString() {
            return String.format("%s{count=%d, avg=%.2fms, min=%dms, max=%dms, total=%dms, slow=%d}",
                    operationName, executionCount, getAverageDuration(), 
                    minDuration == Long.MAX_VALUE ? 0 : minDuration,
                    maxDuration == Long.MIN_VALUE ? 0 : maxDuration,
                    totalDuration, slowCount);
        }
    }
}