package besu.optimization.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration for Background Task Execution
 *
 * Paper reference (Section 3.2):
 * "A ThreadPoolExecutor with 64 worker threads and a bounded queue of 4,000
 * pending tasks processes blockchain calls asynchronously."
 *
 * Key features:
 * - 32-64 worker threads for blockchain calls
 * - Queue capacity of 4000 for burst handling
 * - CallerRunsPolicy for natural backpressure (no request loss)
 * - Tasks are executed AFTER transaction commits
 */
@Slf4j
@Component
public class AsyncConfig {

    private ThreadPoolTaskExecutor executor;

    @PostConstruct
    void init() {
        executor = new ThreadPoolTaskExecutor();
        // Paper reference (Section 3.2):
        // "A ThreadPoolExecutor with 64 worker threads and a bounded queue of 4,000"
        executor.setCorePoolSize(64);
        executor.setMaxPoolSize(64);
        executor.setQueueCapacity(4000);
        executor.setThreadNamePrefix("blockchain-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        // CallerRunsPolicy: When pool is saturated, calling thread executes the task
        // This provides natural backpressure without request loss
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        log.info("AsyncConfig initialized: core={}, max={}, queue={}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    }

    @PreDestroy
    void shutdown() {
        log.info("Shutting down AsyncConfig executor");
        executor.shutdown();
    }

    /**
     * Run task AFTER the current transaction commits.
     *
     * This is crucial for the Transaction Isolation Pattern:
     * - The DB transaction commits first (TX 1)
     * - Then the blockchain call runs in background
     * - DB connection is NOT held during the ~4-10s blockchain wait
     */
    public void runAfterCommit(Runnable task) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            // Register to run after commit
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            submit(task);
                        }
                    }
            );
        } else {
            // No active transaction, run immediately
            submit(task);
        }
    }

    /**
     * Run task immediately (ignoring transaction state)
     */
    public void runAsync(Runnable task) {
        submit(task);
    }

    private void submit(Runnable task) {
        try {
            executor.execute(task);
        } catch (RejectedExecutionException e) {
            // This shouldn't happen with CallerRunsPolicy, but just in case
            log.warn("Task rejected, running in caller thread. active={}, queue={}",
                    executor.getActiveCount(),
                    executor.getThreadPoolExecutor().getQueue().size());
            task.run();
        }
    }

    /**
     * Get executor stats for monitoring
     */
    public ExecutorStats getStats() {
        return new ExecutorStats(
                executor.getActiveCount(),
                executor.getThreadPoolExecutor().getQueue().size(),
                executor.getThreadPoolExecutor().getCompletedTaskCount()
        );
    }

    public record ExecutorStats(int activeThreads, int queueSize, long completedTasks) {}
}
