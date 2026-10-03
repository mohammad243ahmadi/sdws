package com.sdws.wallet.api;

import com.sdws.wallet.api.ApiDtos.ActiveStatusRequest;
import com.sdws.wallet.api.ApiDtos.AdminOverviewResponse;
import com.sdws.wallet.api.ApiDtos.TransactionResponse;
import com.sdws.wallet.api.ApiDtos.UserResponse;
import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.repo.UserRepository;
import com.sdws.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminApiController {

    private final WalletService walletService;
    private final UserRepository users;

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        List<AppUser> allUsers = walletService.allUsers();
        List<com.sdws.wallet.model.WalletTransaction> transactions = walletService.allTransactions(null, null);
        return new AdminOverviewResponse(
                allUsers.size(),
                allUsers.stream().filter(AppUser::isActive).count(),
                transactions.size(),
                walletService.totalBalance(),
                transactions.stream().limit(10).map(TransactionResponse::from).toList());
    }

    @GetMapping("/users")
    public List<UserResponse> users() {
        return walletService.allUsers().stream().map(UserResponse::from).toList();
    }

    @PatchMapping("/users/{id}")
    public ResponseEntity<Void> setUserActive(Authentication authentication, @PathVariable Long id,
                                              @Valid @RequestBody ActiveStatusRequest request) {
        AppUser target = users.findById(id).orElseThrow();
        if (target.isActive() != request.active()) {
            walletService.toggleActive(walletService.current(authentication.getName()), id);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(@RequestParam(required = false) String type,
                                                 @RequestParam(required = false) String phone) {
        return walletService.allTransactions(type, phone).stream().map(TransactionResponse::from).toList();
    }
}