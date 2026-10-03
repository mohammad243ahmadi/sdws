package com.sdws.wallet.repo;

import com.sdws.wallet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByUserOrderByCreatedAtDescIdDesc(AppUser user);
    List<WalletTransaction> findAllByOrderByCreatedAtDescIdDesc();
}
