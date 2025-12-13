package besu.optimization.account;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/**
 * Account Entity
 * Stores account information with blockchain wallet address
 */
@Entity
@Table(name = "accounts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;

    @Column(name = "user_name")
    private String userName;

    @Column(name = "wallet_address")
    private String walletAddress;

    @Column(name = "tx_hash")
    private String txHash;

    @Column(name = "status")
    @Builder.Default
    private Integer status = 0; // 0: PENDING, 1: ACTIVE, 2: FAILED

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Update blockchain information after successful registration
     * Called by BlockchainUpdater with REQUIRES_NEW transaction
     */
    public void updateBlockchain(String walletAddress, String txHash) {
        this.walletAddress = walletAddress;
        this.txHash = txHash;
        this.status = 1; // ACTIVE
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Mark as failed
     */
    public void markFailed() {
        this.status = 2; // FAILED
        this.updatedAt = LocalDateTime.now();
    }
}
