package besu.optimization.account;

import besu.optimization.blockchain.BlockchainService;
import besu.optimization.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Account Service
 *
 * Demonstrates the Transaction Isolation Pattern:
 * 1. TX 1: Quick DB insert (~10ms) - then commit, release DB connection
 * 2. Async blockchain call (~4-10s) - NO DB connection held during wait
 * 3. TX 2: Quick DB update (~5ms) - separate transaction
 *
 * This pattern prevents DB connection pool exhaustion under high load.
 * Without this pattern, a 100-connection pool would be exhausted at just 10-25 TPS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BlockchainService blockchainService;
    private final AsyncConfig asyncConfig;

    /**
     * Create account with Transaction Isolation Pattern
     *
     * The blockchain registration is executed asynchronously AFTER
     * the initial DB transaction commits. This ensures:
     * - DB connections are held only for ~10ms (not 4-10 seconds)
     * - The user gets immediate response
     * - Blockchain registration happens in background
     */
    @Transactional
    public AccountDto createAccount(CreateAccountRequest request) {
        log.info("[createAccount] userId={}", request.userId());

        // Validate
        if (accountRepository.existsByUserId(request.userId())) {
            throw new IllegalArgumentException("User ID already exists: " + request.userId());
        }

        // TX 1: Quick DB insert (~10ms)
        Account account = Account.builder()
                .userId(request.userId())
                .userName(request.userName())
                .status(0) // PENDING
                .build();

        Account saved = accountRepository.save(account);
        log.info("[createAccount] DB saved, id={}", saved.getId());

        // After TX 1 commits, trigger async blockchain registration
        // This runs OUTSIDE the current transaction
        final String userId = saved.getUserId();
        final String userName = saved.getUserName();

        asyncConfig.runAfterCommit(() -> {
            log.info("[bg:register] Starting blockchain registration for {}", userId);
            try {
                // This call takes 4-10 seconds but doesn't hold a DB connection
                blockchainService.registerAccount(userId, userName);
            } catch (Exception e) {
                log.warn("[bg:register] Failed for {}: {}", userId, e.getMessage());
            }
        });

        return AccountDto.from(saved);
    }

    @Transactional(readOnly = true)
    public AccountDto getAccount(String userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + userId));
        return AccountDto.from(account);
    }

    // DTO Records
    public record CreateAccountRequest(String userId, String userName) {}

    public record AccountDto(
            Long id,
            String userId,
            String userName,
            String walletAddress,
            String txHash,
            Integer status
    ) {
        public static AccountDto from(Account account) {
            return new AccountDto(
                    account.getId(),
                    account.getUserId(),
                    account.getUserName(),
                    account.getWalletAddress(),
                    account.getTxHash(),
                    account.getStatus()
            );
        }
    }
}
