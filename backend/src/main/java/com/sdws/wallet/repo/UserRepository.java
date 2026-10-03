package com.sdws.wallet.repo;

import com.sdws.wallet.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByPhone(String phone);
    boolean existsByPhone(String phone);
}
