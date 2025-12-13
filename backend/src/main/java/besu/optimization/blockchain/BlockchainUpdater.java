package besu.optimization.blockchain;

import besu.optimization.account.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blockchain Updater
 *
 * Handles DB updates AFTER blockchain operations complete.
 * Uses REQUIRES_NEW propagation to ensure a separate, short transaction.
 *
 * Key insight from the paper:
 * - Without Transaction Isolation Pattern: DB connection held for 4-10 seconds
 *   -> 100 connections / 4s = only 25 TPS before pool exhaustion
 *
 * - With Transaction Isolation Pattern:
 *   - TX 1 (insert): ~10ms
 *   - Blockchain I/O: ~4-10s (NO DB connection)
 *   - TX 2 (update): ~5ms
 *   -> DB pool no longer a bottleneck
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainUpdater {

    private final AccountRepository accountRepository;

    /**
     * Update account with blockchain information
     *
     * REQUIRES_NEW ensures this runs in a NEW, separate transaction.
     * This transaction is very short (~5ms) and immediately releases
     * the DB connection.
     *
     * timeout=5 seconds for safety (should complete in milliseconds)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 5)
    public void updateBlockchain(String userId, String walletAddress, String txHash) {
        log.info("[BlockchainUpdater] Updating userId={}, wallet={}", userId, walletAddress);

        int rows = accountRepository.updateBlockchain(userId, walletAddress, txHash);

        if (rows == 0) {
            log.warn("[BlockchainUpdater] No rows updated for userId={}", userId);
            throw new IllegalArgumentException("Account not found: " + userId);
        }

        log.info("[BlockchainUpdater] Updated {} rows for userId={}", rows, userId);
    }
}
