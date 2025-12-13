import { ethers, JsonRpcProvider, Wallet } from 'ethers';

/**
 * Blockchain Adapter
 *
 * Connects to Hyperledger Besu via JSON-RPC.
 * Provides core blockchain functionality including:
 * - Wallet creation
 * - Token faucet interaction
 * - Balance queries
 *
 * Features retry logic with exponential backoff for resilient startup.
 */
class BlockchainAdapter {
  public provider: JsonRpcProvider | null = null;
  public wallet: Wallet | null = null;
  private initialized: boolean = false;

  /**
   * Initialize the adapter with retry logic
   *
   * Besu node may take time to start, so we retry connection attempts
   * with exponential backoff.
   */
  async initialize(): Promise<void> {
    if (this.initialized) return;

    const rpcUrl = process.env.BLOCKCHAIN_RPC_URL || 'http://localhost:8545';
    const privateKey = process.env.PRIVATE_KEY;
    const maxRetries = 10;
    const initialDelayMs = 2000;

    console.log(`[BlockchainAdapter] Connecting to ${rpcUrl}...`);

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        // Create provider
        this.provider = new JsonRpcProvider(rpcUrl);

        // Test connection by getting network info
        const network = await this.provider.getNetwork();
        console.log(`[BlockchainAdapter] Connected to network: ${network.name} (chainId: ${network.chainId})`);

        // Create wallet if private key provided
        if (privateKey) {
          this.wallet = new Wallet(privateKey, this.provider);
          console.log(`[BlockchainAdapter] Wallet initialized: ${this.wallet.address}`);
        }

        this.initialized = true;
        console.log('[BlockchainAdapter] Initialization successful');
        return;

      } catch (error) {
        const delayMs = initialDelayMs * Math.pow(2, attempt - 1);
        console.warn(`[BlockchainAdapter] Connection attempt ${attempt}/${maxRetries} failed: ${(error as Error).message}`);

        if (attempt < maxRetries) {
          console.log(`[BlockchainAdapter] Retrying in ${delayMs}ms...`);
          await this.sleep(delayMs);
        } else {
          console.error('[BlockchainAdapter] All connection attempts failed');
          throw error;
        }
      }
    }
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }

  /**
   * Create a new random wallet
   */
  createWallet(): { address: string; privateKey: string } {
    const wallet = ethers.Wallet.createRandom();
    return {
      address: wallet.address,
      privateKey: wallet.privateKey
    };
  }

  /**
   * Get balance of an address
   */
  async getBalance(address: string): Promise<string> {
    if (!this.provider) throw new Error('Provider not initialized');
    const balance = await this.provider.getBalance(address);
    return ethers.formatEther(balance);
  }

  /**
   * Get current block number
   */
  async getBlockNumber(): Promise<number> {
    if (!this.provider) throw new Error('Provider not initialized');
    return await this.provider.getBlockNumber();
  }

  /**
   * Check if address is valid
   */
  isValidAddress(address: string): boolean {
    return ethers.isAddress(address);
  }
}

export const blockchainAdapter = new BlockchainAdapter();
export default blockchainAdapter;
