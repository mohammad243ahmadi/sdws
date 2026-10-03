package com.sdws.wallet.repo;

import com.sdws.wallet.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MoneyRequestRepository extends JpaRepository<MoneyRequest, Long> {
    List<MoneyRequest> findByRequesterOrderByCreatedAtDesc(AppUser requester);
    List<MoneyRequest> findByTargetOrderByCreatedAtDesc(AppUser target);
}
