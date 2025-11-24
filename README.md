# Hyperledger Besu Service Optimization

**Achieving 650+ TPS through Holistic 3-Layer Architecture**

---

## 📄 Paper

Implementation of the paper:  
*"Breaking the Throughput Barrier: 24-Fold Performance Improvement for Hyperledger Besu Through Service-Layer Optimization"*

**Submitted to:** ICBTA 2025 (International Conference on Blockchain Technology and Applications)  
**Conference:** December 12-14, 2025, Kanazawa, Japan

---

## 🚧 Status

**Code is being organized and will be released soon.**

We are currently preparing:
- Java Spring Boot Backend (Virtual Threads + Transaction Isolation)
- Node.js API Gateway (PM2 Cluster Configuration)
- Hyperledger Besu Node Configurations
- Solidity Smart Contracts
- Apache JMeter Test Plans
- Deployment Documentation

**⭐ Star this repository to get notified when the code is released!**

---

## 📊 Performance Highlights

- **650+ TPS** achieved (vs. 25 TPS baseline)
- **27x improvement** over traditional architecture
- **68-140% improvement** over state-of-the-art node-only optimizations
- Validated through 5 independent experimental runs

---

## 🏗️ Architecture

### Layer 1: Java Backend
- Java 21 Virtual Threads
- Transaction Isolation Pattern
- Spring Boot 3.2+

### Layer 2: Node.js Middleware
- PM2 Cluster Mode (32 cores)
- Express API Gateway

### Layer 3: Hyperledger Besu
- Bonsai + Layered Storage
- JVM Tuning (G1GC, 80% heap)
- Performance Profile
- IBFT2 Consensus (4 validators)

---

## 📧 Contact

**Authors:**
- Jinhyeok Kim - druther34@gmail.com
- Changyun Lee - ceo@creativecode.co.kr

**Institution:**  
CreativeCode Research Institute, Seoul, Republic of Korea

**For early access or collaboration inquiries, please contact us via email.**

---

<p align="center">
  <b>⭐ Star this repo to stay updated!</b>
</p>
