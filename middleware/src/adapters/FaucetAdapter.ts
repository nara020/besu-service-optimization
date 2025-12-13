import { ethers, Wallet, Contract } from 'ethers';
import { blockchainAdapter } from './BlockchainAdapter';

/**
 * Faucet Adapter
 *
 * Interacts with the NativeTokenFaucetV1 smart contract.
 * Provides initial ETH funding for new accounts (1 ETH per account).
 *
 * Contract functions used:
 * - open(): Claims initial funding (called by new account)
 * - opened(address): Checks if account has claimed
 * - contractBalance(): Returns faucet balance
 */

// NativeTokenFaucetV1 ABI (minimal)
const FAUCET_ABI = [
  'function open() external',
  'function opened(address) external view returns (bool)',
  'function contractBalance() external view returns (uint256)',
  'function query(address account) external view returns (uint256)',
  'function version() external pure returns (string memory)',
  'function owner() external view returns (address)',
  'function FAUCET_AMOUNT() external view returns (uint256)'
];

class FaucetAdapter {
  private contract: Contract | null = null;
  private contractAddress: string | null = null;

  /**
   * Initialize the faucet adapter
   *
   * Note: Faucet is optional. If FAUCET_CONTRACT_ADDRESS is not set,
   * accounts will be created without initial funding.
   */
  async initialize(): Promise<void> {
    this.contractAddress = process.env.FAUCET_CONTRACT_ADDRESS || null;

    if (!this.contractAddress) {
      console.info('[FaucetAdapter] FAUCET_CONTRACT_ADDRESS not set - accounts will be created without initial funding');
      console.info('[FaucetAdapter] This is normal for demo mode. Set FAUCET_CONTRACT_ADDRESS for production.');
      return;
    }

    if (!blockchainAdapter.wallet) {
      console.warn('[FaucetAdapter] Wallet not initialized - faucet funding disabled');
      return;
    }

    this.contract = new Contract(
      this.contractAddress,
      FAUCET_ABI,
      blockchainAdapter.wallet
    );

    try {
      const version = await this.contract.version();
      const balance = await this.contract.contractBalance();
      console.log(`Faucet initialized: v${version}, balance: ${ethers.formatEther(balance)} ETH`);
    } catch (error) {
      console.warn('Faucet contract info retrieval failed:', error);
    }
  }

  /**
   * Fund a new account
   *
   * The new account calls open() on the faucet contract to receive 1 ETH.
   * This requires the account's private key to sign the transaction.
   */
  async fundNewAccount(walletAddress: string, privateKey: string): Promise<{
    success: boolean;
    txHash?: string;
    error?: string;
  }> {
    if (!this.contractAddress || !blockchainAdapter.provider) {
      return { success: false, error: 'Faucet not initialized' };
    }

    try {
      // Create wallet for the new account
      const accountWallet = new Wallet(privateKey, blockchainAdapter.provider);

      // Create contract instance with new account as signer
      const accountContract = new Contract(
        this.contractAddress,
        FAUCET_ABI,
        accountWallet
      );

      // Call open() to claim initial funding
      const tx = await accountContract.open();
      await tx.wait();

      console.log(`Funded account ${walletAddress}, txHash: ${tx.hash}`);

      return {
        success: true,
        txHash: tx.hash
      };
    } catch (error) {
      const message = (error as Error).message;
      console.error(`Failed to fund account ${walletAddress}:`, message);

      return {
        success: false,
        error: message
      };
    }
  }

  /**
   * Check if an account has been funded
   */
  async isAccountFunded(address: string): Promise<boolean> {
    if (!this.contract) return false;

    try {
      return await this.contract.opened(address);
    } catch {
      return false;
    }
  }

  /**
   * Get faucet contract balance
   */
  async getContractBalance(): Promise<string> {
    if (!this.contract) return '0';

    try {
      const balance = await this.contract.contractBalance();
      return ethers.formatEther(balance);
    } catch {
      return '0';
    }
  }
}

export const faucetAdapter = new FaucetAdapter();
export default faucetAdapter;
