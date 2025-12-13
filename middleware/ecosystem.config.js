/**
 * PM2 Ecosystem Configuration
 *
 * Paper reference (Section 3.3):
 * "PM2 Cluster Mode forks the Node.js application into multiple worker
 * processes (one for each CPU core), allowing a load balancer to distribute
 * incoming HTTP requests across all available cores."
 *
 * Key settings:
 * - exec_mode: 'cluster' - Enables PM2 clustering
 * - instances: 'max' - One worker per CPU core
 * - UV_THREADPOOL_SIZE: 128 - Expands libuv thread pool for I/O
 */
module.exports = {
  apps: [{
    name: 'besu-middleware',
    script: './dist/server.js',

    // ==========================================================================
    // Cluster Mode Configuration
    // ==========================================================================
    // Paper: "It forks one worker process per CPU core, transforming Node.js
    // from a single-core bottleneck into a horizontally scalable gateway."
    exec_mode: 'cluster',
    instances: 'max',  // One worker per CPU core

    // ==========================================================================
    // Memory Management
    // ==========================================================================
    max_memory_restart: '2G',  // Restart worker if exceeds 2GB

    // ==========================================================================
    // Node.js Options
    // ==========================================================================
    node_args: [
      '--max-old-space-size=2048',  // 2GB heap per worker
      '--max-semi-space-size=128',
      '--optimize-for-size',
      '--expose-gc',
      '--unhandled-rejections=strict'
    ],

    // ==========================================================================
    // Environment Variables
    // ==========================================================================
    env: {
      NODE_ENV: 'production',
      // Paper: "UV_THREADPOOL_SIZE=128 - I/O thread pool expansion"
      UV_THREADPOOL_SIZE: 128,
      NODE_OPTIONS: '--max-http-header-size=16384'
    },

    // ==========================================================================
    // Logging
    // ==========================================================================
    error_file: './logs/pm2-error.log',
    out_file: './logs/pm2-out.log',
    log_date_format: 'YYYY-MM-DD HH:mm:ss Z',
    merge_logs: true,

    // ==========================================================================
    // Restart Policy
    // ==========================================================================
    autorestart: true,
    watch: false,
    max_restarts: 10,
    min_uptime: '10s',

    // ==========================================================================
    // Graceful Shutdown
    // ==========================================================================
    kill_timeout: 5000,
    listen_timeout: 3000,
    shutdown_with_message: true
  }]
};
