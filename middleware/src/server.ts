import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { accountRoutes } from './routes/accounts';
import { blockchainAdapter } from './adapters/BlockchainAdapter';

const app = express();
const PORT = process.env.PORT || 3000;

// =============================================================================
// Middleware
// =============================================================================
app.use(helmet());
app.use(cors());
app.use(morgan('combined'));
app.use(express.json());

// =============================================================================
// Routes
// =============================================================================
app.use('/api/accounts', accountRoutes);

// Health check
app.get('/health', (req, res) => {
  res.json({
    status: 'UP',
    service: 'besu-middleware',
    workerId: process.env.pm_id || 'standalone',
    uptime: process.uptime()
  });
});

// Root endpoint
app.get('/', (req, res) => {
  res.json({
    name: 'Besu Middleware API',
    version: '1.0.0',
    description: 'Layer 2: Node.js API Gateway with PM2 Cluster Mode',
    endpoints: {
      health: '/health',
      accounts: '/api/accounts'
    }
  });
});

// =============================================================================
// Error Handler
// =============================================================================
app.use((err: Error, req: express.Request, res: express.Response, next: express.NextFunction) => {
  console.error('Error:', err.message);
  res.status(500).json({
    success: false,
    message: 'Internal server error',
    error: err.message
  });
});

// =============================================================================
// Start Server
// =============================================================================
async function start() {
  try {
    // Initialize blockchain adapter
    await blockchainAdapter.initialize();
    console.log('Blockchain adapter initialized');

    app.listen(PORT, () => {
      console.log(`Server running on port ${PORT}`);
      console.log(`Worker ID: ${process.env.pm_id || 'standalone'}`);
      console.log(`UV_THREADPOOL_SIZE: ${process.env.UV_THREADPOOL_SIZE || 4}`);
    });
  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
}

start();

export default app;
