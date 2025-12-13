-- =============================================================================
-- Besu Service Optimization - Database Schema
-- =============================================================================
-- This schema supports the Transaction Isolation Pattern demonstrated in the paper.
-- Accounts are created first (TX 1), then blockchain info is updated later (TX 2).

-- Accounts table
CREATE TABLE IF NOT EXISTS accounts (
    id SERIAL PRIMARY KEY,
    user_id VARCHAR(100) UNIQUE NOT NULL,
    user_name VARCHAR(200),
    wallet_address VARCHAR(42),
    tx_hash VARCHAR(66),
    status INTEGER DEFAULT 0,  -- 0: PENDING, 1: ACTIVE, 2: FAILED
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index for faster lookups
CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_wallet_address ON accounts(wallet_address);
CREATE INDEX IF NOT EXISTS idx_accounts_status ON accounts(status);

-- Comments
COMMENT ON TABLE accounts IS 'User accounts with blockchain wallet integration';
COMMENT ON COLUMN accounts.status IS '0: PENDING (DB saved, blockchain pending), 1: ACTIVE (blockchain confirmed), 2: FAILED';
COMMENT ON COLUMN accounts.wallet_address IS 'Ethereum wallet address (0x...)';
COMMENT ON COLUMN accounts.tx_hash IS 'Blockchain transaction hash for initial funding';

-- =============================================================================
-- Transaction Isolation Pattern Explanation:
-- =============================================================================
-- 1. TX 1 (AccountService.createAccount):
--    - INSERT INTO accounts (user_id, user_name, status=0)
--    - COMMIT immediately (~10ms)
--    - DB connection released
--
-- 2. Async blockchain call (BlockchainService.registerAccount):
--    - Takes 4-10 seconds
--    - NO DB connection held during this wait
--
-- 3. TX 2 (BlockchainUpdater.updateBlockchain):
--    - UPDATE accounts SET wallet_address=?, tx_hash=?, status=1
--    - New short transaction (~5ms)
--    - DB connection released immediately
-- =============================================================================

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO besu;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO besu;
