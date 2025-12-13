package besu.optimization.account;

import besu.optimization.blockchain.BlockchainService;
import besu.optimization.config.AsyncConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AccountService Unit Tests
 *
 * Tests the Transaction Isolation Pattern implementation:
 * - TX 1: Quick DB insert
 * - Async blockchain call (mocked)
 * - TX 2: Quick DB update (via BlockchainUpdater)
 */
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BlockchainService blockchainService;

    @Mock
    private AsyncConfig asyncConfig;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, blockchainService, asyncConfig);
    }

    @Test
    @DisplayName("createAccount - should save account and trigger async blockchain registration")
    void createAccount_Success() {
        // Given
        String userId = "testUser001";
        String userName = "Test User";
        var request = new AccountService.CreateAccountRequest(userId, userName);

        Account savedAccount = Account.builder()
                .id(1L)
                .userId(userId)
                .userName(userName)
                .status(0)
                .build();

        when(accountRepository.existsByUserId(userId)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);

        // Capture the async task
        doAnswer(invocation -> {
            // Just verify runAfterCommit is called, don't execute the task
            return null;
        }).when(asyncConfig).runAfterCommit(any(Runnable.class));

        // When
        AccountService.AccountDto result = accountService.createAccount(request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.userName()).isEqualTo(userName);
        assertThat(result.status()).isEqualTo(0); // PENDING

        // Verify DB save was called
        verify(accountRepository).save(any(Account.class));

        // Verify async blockchain registration was triggered
        verify(asyncConfig).runAfterCommit(any(Runnable.class));
    }

    @Test
    @DisplayName("createAccount - should throw exception for duplicate userId")
    void createAccount_DuplicateUserId_ThrowsException() {
        // Given
        String userId = "existingUser";
        var request = new AccountService.CreateAccountRequest(userId, "Test");

        when(accountRepository.existsByUserId(userId)).thenReturn(true);

        // When & Then
        assertThatThrownBy(() -> accountService.createAccount(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User ID already exists");

        // Verify no save attempt
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("getAccount - should return account when found")
    void getAccount_Found() {
        // Given
        String userId = "testUser";
        Account account = Account.builder()
                .id(1L)
                .userId(userId)
                .userName("Test")
                .walletAddress("0x1234567890123456789012345678901234567890")
                .txHash("0xabcdef")
                .status(1)
                .build();

        when(accountRepository.findByUserId(userId)).thenReturn(Optional.of(account));

        // When
        AccountService.AccountDto result = accountService.getAccount(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.walletAddress()).isEqualTo("0x1234567890123456789012345678901234567890");
        assertThat(result.status()).isEqualTo(1); // ACTIVE
    }

    @Test
    @DisplayName("getAccount - should throw exception when not found")
    void getAccount_NotFound_ThrowsException() {
        // Given
        String userId = "nonExistentUser";
        when(accountRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> accountService.getAccount(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Account not found");
    }
}
