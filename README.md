# Besu Service Optimization

[![CI](https://github.com/nara020/besu-service-optimization/actions/workflows/ci.yml/badge.svg)](https://github.com/nara020/besu-service-optimization/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Node.js](https://img.shields.io/badge/Node.js-20-339933?logo=node.js&logoColor=white)](https://nodejs.org/)
[![Besu](https://img.shields.io/badge/Hyperledger-Besu-2F3134?logo=hyperledger&logoColor=white)](https://besu.hyperledger.org/)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)

**27-Fold Performance Improvement for Hyperledger Besu Through Service-Layer Optimization**

This repository provides the open-source implementation accompanying our research paper presented at ICBTA 2025.

## Paper Abstract

When backend services integrate with Hyperledger Besu, the multi-second transaction finality wait creates significant I/O blocking challenges, causing thread exhaustion and database connection pool depletion. We present a holistic three-layer optimization methodology that achieves **678 TPS** (a 27-fold improvement over the baseline 25 TPS) with 0.6% coefficient of variation across five independent runs.

## Architecture Overview

```
+-------------------------------------------------------------+
|                 Load Generator (JMeter)                      |
|                    600+ TPS Load                             |
+---------------------+---------------------------------------+
                      | HTTP Requests
                      v
+-------------------------------------------------------------+
|           Layer 1: Backend Service (Java Spring Boot)        |
|                                                              |
|  - Virtual Threads (spring.threads.virtual.enabled=true)     |
|  - Transaction Isolation Pattern                             |
|  - Async Thread Pool (64 workers, 4000 queue)               |
|  - Connection Pool (500 max connections)                     |
+---------------------+---------------------------------------+
                      | REST API (Async HTTP)
                      v
+-------------------------------------------------------------+
|         Layer 2: Middleware (Node.js API Gateway)            |
|                                                              |
|  - PM2 Cluster Mode (instances: 'max')                       |
|  - UV_THREADPOOL_SIZE=128                                    |
|  - Ethers.js for Web3 RPC                                    |
+---------------------+---------------------------------------+
                      | Web3 RPC (~4-10s I/O Wait)
                      v
+-------------------------------------------------------------+
|           Layer 3: Blockchain (Hyperledger Besu)             |
|                                                              |
|  - IBFT2 Consensus (4 validators)                            |
|  - Bonsai + Layered Storage                                  |
|  - G1GC Tuning (-XX:MaxGCPauseMillis=100)                   |
|  - Layered TxPool (max-capacity=5B)                          |
+-------------------------------------------------------------+
```

## Key Optimizations

### Layer 1: Java Backend (Spring Boot 3.2+)

1. **Virtual Threads (JEP 444)**: Handles thousands of concurrent requests without thread exhaustion
2. **Transaction Isolation Pattern**: Separates short DB transactions from long blockchain I/O waits

```java
// Transaction Isolation Pattern
@Transactional  // TX 1: Quick DB insert (~10ms)
public Account createAccount(request) {
    Account account = repository.save(new Account(request));
    // Commit here - DB connection released
}
// NO @Transactional - Blockchain call (~4-10s)
String txHash = blockchainClient.register(account);

@Transactional(propagation = REQUIRES_NEW)  // TX 2: Quick update (~5ms)
public void updateTxHash(accountId, txHash) {
    repository.updateTxHash(accountId, txHash);
}
```

### Layer 2: Node.js Middleware

```javascript
// ecosystem.config.js - PM2 Cluster Mode
module.exports = {
  apps: [{
    name: 'besu-api-gateway',
    exec_mode: 'cluster',
    instances: 'max',  // One per CPU core
    env: { UV_THREADPOOL_SIZE: 128 }
  }]
};
```

### Layer 3: Besu Configuration

| Parameter | Default | Optimized | Impact |
|-----------|---------|-----------|--------|
| Performance Profile | N/A | performance | Parallel validation |
| tx-pool-layer-max-capacity | 12.5M | 5B | Burst capacity |
| rpc-http-max-connections | 80 | 50,000 | HTTP capacity |
| MaxRAMPercentage | 25% | 80% | Heap memory |
| MaxGCPauseMillis | 200ms | 100ms | GC pause |

## Quick Start

### Option 1: Local Demo (Single Machine)

Best for quick testing and development.

```bash
cd deployment/local
docker compose up -d
```

This starts:
- 1 Besu node (simplified for demo)
- PostgreSQL database
- Node.js middleware
- Java backend (Virtual Threads enabled)

**Test the API:**
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"userId": "user001", "userName": "Test User"}'
```

### Option 2: Cloud Deployment (Production)

For production with multiple servers (matches paper environment).

```bash
cd deployment/cloud

# Install dependencies
pip install -r requirements.txt

# Generate network configuration
python setup.py
```

This generates:
- Genesis file with IBFT2 consensus
- Validator keys for 4 nodes
- Docker Compose files for each node
- Deployment instructions

**Deploy to servers:**
```bash
# On each server
docker compose -f node1.yml up -d  # (node1.yml, node2.yml, etc.)
```

## Prerequisites

- Docker & Docker Compose
- 16GB+ RAM recommended
- Python 3.8+ (for cloud deployment setup)

## Project Structure

```
opensource/
├── README.md
├── backend/                    # Layer 1: Java Spring Boot
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/java/besu/optimization/
│       ├── account/            # Account service with Transaction Isolation
│       ├── blockchain/         # Blockchain integration
│       └── config/             # Async & REST client config
├── middleware/                 # Layer 2: Node.js API Gateway
│   ├── Dockerfile
│   ├── ecosystem.config.js     # PM2 cluster configuration
│   └── src/adapters/           # Blockchain adapters (ethers.js)
├── blockchain/                 # Layer 3: Besu Network
│   ├── config/
│   │   ├── genesis.json        # IBFT2 genesis
│   │   └── keys/               # Validator keys
│   └── contracts/
│       └── NativeTokenFaucetV1.sol
├── database/
│   └── init.sql                # PostgreSQL schema
└── deployment/
    ├── local/                  # Single-machine demo
    │   └── docker-compose.yml
    └── cloud/                  # Multi-server production
        ├── setup.py            # Network generator
        └── requirements.txt
```

## Performance Results

| Scenario | Configuration | TPS | Improvement |
|----------|--------------|-----|-------------|
| S1 (Baseline) | Platform Threads, Single Node.js | 25 | 1x |
| S2 (+L1) | Virtual Threads | 65 | 2.6x |
| S3 (+L2) | + PM2 Cluster | 375 | 15x |
| S4 (+L3) | + Besu Optimizations | 678 | **27x** |

## Citation

If you use this work, please cite:

```bibtex
@inproceedings{kim2025breaking,
  title={Breaking the Throughput Barrier: 27-Fold Performance Improvement
         for Hyperledger Besu Through Service-Layer Optimization},
  author={Kim, Jinhyeok and Lee, Changyun},
  booktitle={International Conference on Blockchain Technology and Applications (ICBTA)},
  year={2025}
}
```

## License

MIT License - See [LICENSE](LICENSE) for details.

## Authors

- Jinhyeok Kim - CreativeCode Research Institute (nara020@naver.com)
- Changyun Lee - CreativeCode Research Institute

## Acknowledgments

This work was supported by the National IT Industry Promotion Agency (NIPA), Korea, under the "Promising SaaS Development and Incubation Support" program (NIPA-2025-PJT-25-033032).
