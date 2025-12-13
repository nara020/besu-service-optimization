package besu.optimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Besu Service Optimization Demo Application
 *
 * This application demonstrates the 3-layer optimization methodology
 * described in "Breaking the Throughput Barrier: 27-Fold Performance
 * Improvement for Hyperledger Besu Through Service-Layer Optimization"
 *
 * Key optimizations in Layer 1 (this backend):
 * 1. Virtual Threads (spring.threads.virtual.enabled=true)
 * 2. Transaction Isolation Pattern
 * 3. Async Thread Pool with backpressure management
 * 4. HTTP Connection Pooling (500 connections)
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
