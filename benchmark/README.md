# Load Testing with Apache JMeter

This directory contains JMeter test plans used to validate the 678 TPS throughput claims from our research paper.

## Prerequisites

### 1. Install Apache JMeter

Download JMeter 5.6+ from the official site:
- **Download**: https://jmeter.apache.org/download_jmeter.cgi
- Extract and run `bin/jmeter.bat` (Windows) or `bin/jmeter` (Linux/Mac)

### 2. Install Required Plugin

The test plan uses **Concurrency Thread Group** for realistic load simulation.

**Option A: JMeter Plugins Manager (Recommended)**
1. Download Plugins Manager: https://jmeter-plugins.org/install/Install/
2. Put `jmeter-plugins-manager-x.x.jar` into `lib/ext` folder
3. Restart JMeter
4. Go to `Options` → `Plugins Manager`
5. Search and install: **"Concurrency Thread Group"**

**Option B: Manual Installation**
1. Download from: https://jmeter-plugins.org/wiki/ConcurrencyThreadGroup/
2. Put JAR files into JMeter's `lib/ext` folder
3. Restart JMeter

## Test Plans

### `load-test.jmx`

Contains 4 test scenarios (enable one at a time):

| Scenario | Description | Target TPS |
|----------|-------------|------------|
| Service Write Test | Backend → Middleware → Blockchain (full stack) | 678 |
| Service Read Test | Backend read-only operations | 2,200+ |
| Blockchain Write Test | Direct Middleware → Blockchain | 700 |
| Blockchain Read Test | Direct Middleware read | 2,200+ |

## Configuration

Before running, configure the target server:

1. Open `load-test.jmx` in JMeter
2. Find **User Defined Variables** or edit HTTP Request defaults:
   - `HOST`: Your backend server (default: `localhost`)
   - `PORT`: Backend port (default: `8080`)
   - `MIDDLEWARE_HOST`: Middleware server (default: `localhost`)
   - `MIDDLEWARE_PORT`: Middleware port (default: `3000`)

## Running Tests

### GUI Mode (for debugging)
```bash
# Windows
jmeter.bat -t load-test.jmx

# Linux/Mac
./jmeter -t load-test.jmx
```

### CLI Mode (for actual load testing)
```bash
# Run test and generate report
jmeter -n -t load-test.jmx -l results.jtl -e -o report/

# Parameters:
# -n : Non-GUI mode
# -t : Test plan file
# -l : Results file
# -e : Generate HTML report
# -o : Report output directory
```

## Expected Results

With the optimized configuration from our paper:

| Metric | Write Operations | Read Operations |
|--------|-----------------|-----------------|
| Throughput | 678 TPS | 2,200+ TPS |
| Avg Latency | ~4.67s | <20ms |
| Error Rate | 0% | 0% |
| P99 Latency | ~6.15s | <50ms |

## Test Environment (Paper Reference)

Our validated results were achieved with:

| Layer | Specification |
|-------|--------------|
| L1: Backend | 8 vCPU, 32GB RAM, Java 21 |
| L2: Middleware | 64 vCPU, 128GB RAM, Node.js 18 |
| L3: Blockchain | 4x Besu nodes (16 vCPU, 128GB each) |

## Troubleshooting

### "Could not find class com.blazemeter.jmeter.threads.concurrency.ConcurrencyThreadGroup"
→ Install Concurrency Thread Group plugin (see Prerequisites)

### Low throughput on local machine
→ Local Docker setup is for demo only. Full performance requires cloud deployment with specs above.

### Connection refused errors
→ Ensure all services are running: `docker compose ps`

## References

- [JMeter Best Practices](https://jmeter.apache.org/usermanual/best-practices.html)
- [Concurrency Thread Group Documentation](https://jmeter-plugins.org/wiki/ConcurrencyThreadGroup/)
- [Our Paper: Breaking the Throughput Barrier](../README.md)
