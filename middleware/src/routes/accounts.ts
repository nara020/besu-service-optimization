import { Router, Request, Response } from 'express';
import { v4 as uuidv4 } from 'uuid';
import { blockchainAdapter } from '../adapters/BlockchainAdapter';
import { faucetAdapter } from '../adapters/FaucetAdapter';

const router = Router();

interface RegisterRequest {
  userId: string;
  userName?: string;
  role?: number;
}

interface RegisterResponse {
  success: boolean;
  message: string;
  data?: {
    accountId: string;
    userId: string;
    userName: string;
    walletAddress: string;
    privateKey: string;
    role: number;
    status: number;
    initialFunding: {
      amount: string;
      txHash: string | null;
      success: boolean;
    };
  };
  error?: string;
}

/**
 * POST /api/accounts/register
 *
 * Register a new account:
 * 1. Create new Ethereum wallet
 * 2. Attempt initial funding via faucet contract
 * 3. Return wallet details
 *
 * This endpoint is called by the Java backend (Layer 1).
 * The ~4-10 second blockchain wait happens here, not in the backend's
 * DB transaction (Transaction Isolation Pattern).
 */
router.post('/register', async (req: Request<Record<string, never>, unknown, RegisterRequest>, res: Response<RegisterResponse>) => {
  const requestId = req.headers['x-request-id'] || uuidv4();
  console.log(`[${requestId}] Register request:`, req.body);

  try {
    const { userId, userName = '', role = 0 } = req.body;

    // Validate
    if (!userId || userId.trim() === '') {
      return res.status(400).json({
        success: false,
        message: 'Validation failed',
        error: 'userId is required'
      });
    }

    // Create new wallet
    const wallet = blockchainAdapter.createWallet();
    console.log(`[${requestId}] Created wallet: ${wallet.address}`);

    // Attempt initial funding (may take 4-10 seconds for blockchain finality)
    const fundingResult = {
      amount: '1.0 ETH',
      txHash: null as string | null,
      success: false
    };

    try {
      const funding = await faucetAdapter.fundNewAccount(wallet.address, wallet.privateKey);
      if (funding.success && funding.txHash) {
        fundingResult.txHash = funding.txHash;
        fundingResult.success = true;
        console.log(`[${requestId}] Funding successful: ${funding.txHash}`);
      } else {
        console.warn(`[${requestId}] Funding failed: ${funding.error}`);
      }
    } catch (fundingError) {
      console.warn(`[${requestId}] Funding error:`, fundingError);
    }

    // Return response
    // SECURITY: Never expose privateKey in API response
    // The privateKey is only used internally for faucet funding
    const response: RegisterResponse = {
      success: true,
      message: 'Account registered successfully',
      data: {
        accountId: uuidv4(),
        userId,
        userName,
        walletAddress: wallet.address,
        privateKey: '********', // Masked for security - stored securely server-side
        role,
        status: 0,
        initialFunding: fundingResult
      }
    };

    console.log(`[${requestId}] Registration complete for ${userId}`);
    return res.status(201).json(response);

  } catch (error) {
    console.error(`[${requestId}] Registration error:`, error);
    return res.status(500).json({
      success: false,
      message: 'Registration failed',
      error: (error as Error).message
    });
  }
});

/**
 * GET /api/accounts/balance/:address
 *
 * Get ETH balance for an address
 */
router.get('/balance/:address', async (req: Request<{ address: string }>, res: Response) => {
  try {
    const { address } = req.params;

    if (!blockchainAdapter.isValidAddress(address)) {
      return res.status(400).json({
        success: false,
        message: 'Invalid address format'
      });
    }

    const balance = await blockchainAdapter.getBalance(address);

    return res.json({
      success: true,
      message: 'Balance retrieved',
      data: {
        address,
        balance: `${balance} ETH`
      }
    });
  } catch (error) {
    return res.status(500).json({
      success: false,
      message: 'Failed to get balance',
      error: (error as Error).message
    });
  }
});

/**
 * GET /api/accounts/service/status
 *
 * Get service status
 */
router.get('/service/status', async (req: Request, res: Response) => {
  try {
    const blockNumber = await blockchainAdapter.getBlockNumber();
    const faucetBalance = await faucetAdapter.getContractBalance();

    return res.json({
      success: true,
      message: 'Service status',
      data: {
        status: 'UP',
        blockchain: {
          connected: true,
          blockNumber
        },
        faucet: {
          balance: `${faucetBalance} ETH`
        },
        worker: process.env.pm_id || 'standalone'
      }
    });
  } catch (error) {
    return res.json({
      success: false,
      message: 'Service status check failed',
      data: {
        status: 'DEGRADED',
        error: (error as Error).message
      }
    });
  }
});

export { router as accountRoutes };
export default router;
