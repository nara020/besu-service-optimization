package besu.optimization.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByUserId(String userId);

    boolean existsByUserId(String userId);

    /**
     * Direct update query for better performance
     * Used by BlockchainUpdater for quick DB updates
     */
    @Modifying
    @Query("UPDATE Account a SET a.walletAddress = :wallet, a.txHash = :txHash, " +
           "a.status = 1, a.updatedAt = CURRENT_TIMESTAMP WHERE a.userId = :userId")
    int updateBlockchain(@Param("userId") String userId,
                         @Param("wallet") String wallet,
                         @Param("txHash") String txHash);
}
