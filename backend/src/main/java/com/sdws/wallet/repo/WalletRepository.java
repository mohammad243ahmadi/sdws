package com.sdws.wallet.repo;

import com.sdws.wallet.model.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByUserId(Long userId);

    /** Row lock so concurrent operations cannot corrupt the balance. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.user.id = :uid")
    Optional<Wallet> findByUserIdForUpdate(@Param("uid") Long userId);
}
