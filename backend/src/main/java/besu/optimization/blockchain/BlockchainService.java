package besu.optimization.blockchain;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;
import java.util.UUID;

/**
 * Blockchain Service
 *
 * Calls the Node.js middleware layer (Layer 2) to interact with Besu.
 * Implements retry logic with exponential backoff for resilience.
 *
 * Key features:
 * - 3 retry attempts with exponential backoff (200ms -> 400ms -> 800ms)
 * - Handles 5xx and 429 errors with retries
 * - Uses non-blocking RestClient with connection pooling
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainService {

    private final RestClient middlewareRestClient;
    private final ObjectMapper objectMapper;
    private final BlockchainUpdater blockchainUpdater;

    private static final int MAX_ATTEMPTS = 3;
    private static final long INITIAL_BACKOFF_MS = 200L;

    /**
     * Register account on blockchain via middleware
     *
     * This method is called asynchronously from AccountService.
     * The ~4-10 second blockchain wait does NOT hold a DB connection
     * because of the Transaction Isolation Pattern.
     */
    public boolean registerAccount(String userId, String userName) {
        final String reqId = UUID.randomUUID().toString();
        log.info("[blockchain/register] reqId={}, userId={}", reqId, userId);

        Map<String, Object> request = Map.of(
                "userId", userId,
                "userName", userName != null ? userName : "",
                "role", 0
        );

        long backoffMs = INITIAL_BACKOFF_MS;

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                var response = middlewareRestClient.post()
                        .uri("/api/accounts/register")
                        .header("X-Request-Id", reqId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .body(request)
                        .retrieve()
                        .toEntity(String.class);

                var status = response.getStatusCode();
                var body = response.getBody();

                log.info("[blockchain/register] reqId={}, attempt={}, status={}", reqId, attempt, status);

                if (status.is2xxSuccessful() && body != null) {
                    return handleSuccessResponse(reqId, userId, body);
                }

                // Retry on 5xx or 429
                if (status.is5xxServerError() || status.value() == 429) {
                    if (attempt < MAX_ATTEMPTS) {
                        sleepQuietly(backoffMs);
                        backoffMs *= 2;
                        continue;
                    }
                }

                log.warn("[blockchain/register] reqId={}, failed with status={}", reqId, status);
                return false;

            } catch (RestClientResponseException e) {
                log.error("[blockchain/register] reqId={}, attempt={}, HTTP error: {}",
                        reqId, attempt, e.getStatusCode());

                int code = e.getStatusCode().value();
                if ((code >= 500 || code == 429) && attempt < MAX_ATTEMPTS) {
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                return false;

            } catch (Exception e) {
                log.error("[blockchain/register] reqId={}, attempt={}, error: {}",
                        reqId, attempt, e.getMessage());

                if (attempt < MAX_ATTEMPTS) {
                    sleepQuietly(backoffMs);
                    backoffMs *= 2;
                    continue;
                }
                return false;
            }
        }

        return false;
    }

    private boolean handleSuccessResponse(String reqId, String userId, String body) {
        try {
            JsonNode root = objectMapper.readTree(body);

            if (root.has("success") && root.get("success").asBoolean()) {
                JsonNode data = root.get("data");
                if (data != null) {
                    String walletAddress = data.has("walletAddress")
                            ? data.get("walletAddress").asText() : null;

                    String txHash = null;
                    if (data.has("initialFunding")) {
                        JsonNode funding = data.get("initialFunding");
                        txHash = funding.has("txHash") ? funding.get("txHash").asText() : null;
                    }

                    if (walletAddress != null) {
                        // TX 2: Quick DB update with separate transaction
                        blockchainUpdater.updateBlockchain(userId, walletAddress, txHash);
                        log.info("[blockchain/register] Success! wallet={}, txHash={}",
                                walletAddress, txHash);
                        return true;
                    }
                }
            }

            log.warn("[blockchain/register] reqId={}, success=false in response", reqId);
            return false;

        } catch (Exception e) {
            log.error("[blockchain/register] reqId={}, parse error: {}", reqId, e.getMessage());
            return false;
        }
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
