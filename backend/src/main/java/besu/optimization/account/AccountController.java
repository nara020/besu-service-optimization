package besu.optimization.account;

import besu.optimization.account.AccountService.AccountDto;
import besu.optimization.account.AccountService.CreateAccountRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * Create new account
     * Triggers async blockchain registration with Transaction Isolation Pattern
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountDto>> createAccount(
            @RequestBody CreateAccountRequest request) {

        log.info("[POST /api/accounts] userId={}", request.userId());

        AccountDto account = accountService.createAccount(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(account, "Account created. Blockchain registration in progress."));
    }

    /**
     * Get account by userId
     */
    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<AccountDto>> getAccount(@PathVariable String userId) {
        AccountDto account = accountService.getAccount(userId);
        return ResponseEntity.ok(ApiResponse.success(account, "Account retrieved"));
    }

    /**
     * Health check
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "besu-backend",
                "virtualThreads", String.valueOf(Thread.currentThread().isVirtual())
        ));
    }

    // Generic API Response wrapper
    public record ApiResponse<T>(
            boolean success,
            String message,
            T data
    ) {
        public static <T> ApiResponse<T> success(T data, String message) {
            return new ApiResponse<>(true, message, data);
        }

        public static <T> ApiResponse<T> error(String message) {
            return new ApiResponse<>(false, message, null);
        }
    }

    // Global exception handler
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("Unexpected error", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Internal server error"));
    }
}
