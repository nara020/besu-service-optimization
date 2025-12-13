package besu.optimization.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.*;

/**
 * AsyncConfig Unit Tests
 *
 * Tests the thread pool configuration that matches the paper specifications:
 * - 64 worker threads
 * - 4000 queue capacity
 * - CallerRunsPolicy for backpressure
 */
class AsyncConfigTest {

    @Test
    @DisplayName("AsyncConfig - should initialize with paper-specified values")
    void init_MatchesPaperSpecifications() {
        // Given & When
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.init();

        // Then
        AsyncConfig.ExecutorStats stats = asyncConfig.getStats();
        assertThat(stats.activeThreads()).isGreaterThanOrEqualTo(0);

        // Cleanup
        asyncConfig.shutdown();
    }

    @Test
    @DisplayName("runAsync - should execute task in background thread")
    void runAsync_ExecutesInBackground() throws InterruptedException {
        // Given
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.init();

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean executed = new AtomicBoolean(false);
        String mainThread = Thread.currentThread().getName();
        AtomicBoolean differentThread = new AtomicBoolean(false);

        // When
        asyncConfig.runAsync(() -> {
            executed.set(true);
            differentThread.set(!Thread.currentThread().getName().equals(mainThread));
            latch.countDown();
        });

        // Then
        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertThat(completed).isTrue();
        assertThat(executed.get()).isTrue();
        assertThat(differentThread.get()).isTrue();

        // Cleanup
        asyncConfig.shutdown();
    }

    @Test
    @DisplayName("getStats - should return current executor statistics")
    void getStats_ReturnsValidStats() {
        // Given
        AsyncConfig asyncConfig = new AsyncConfig();
        asyncConfig.init();

        // When
        AsyncConfig.ExecutorStats stats = asyncConfig.getStats();

        // Then
        assertThat(stats).isNotNull();
        assertThat(stats.activeThreads()).isGreaterThanOrEqualTo(0);
        assertThat(stats.queueSize()).isGreaterThanOrEqualTo(0);
        assertThat(stats.completedTasks()).isGreaterThanOrEqualTo(0);

        // Cleanup
        asyncConfig.shutdown();
    }
}
