package com.sdws.wallet.api;

import com.sdws.wallet.api.ApiDtos.AmountRequest;
import com.sdws.wallet.api.ApiDtos.BillPaymentRequest;
import com.sdws.wallet.api.ApiDtos.MoneyRequestCreate;
import com.sdws.wallet.api.ApiDtos.MoneyRequestResponse;
import com.sdws.wallet.api.ApiDtos.MoneyRequestsResponse;
import com.sdws.wallet.api.ApiDtos.MobileTopupRequest;
import com.sdws.wallet.api.ApiDtos.ProfileUpdateRequest;
import com.sdws.wallet.api.ApiDtos.TransactionResponse;
import com.sdws.wallet.api.ApiDtos.TransferRequest;
import com.sdws.wallet.api.ApiDtos.UserResponse;
import com.sdws.wallet.api.ApiDtos.WalletResponse;
import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WalletApiController {

    private final WalletService walletService;

    @GetMapping("/wallet")
    public WalletResponse wallet(Authentication authentication) {
        return WalletResponse.from(walletService.wallet(current(authentication)));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> transactions(Authentication authentication,
                                                  @RequestParam(required = false) String type) {
        return walletService.history(current(authentication), type).stream()
                .map(TransactionResponse::from)
                .toList();
    }

    @PostMapping("/transfers")
    public ResponseEntity<Void> transfer(Authentication authentication, @Valid @RequestBody TransferRequest request) {
        walletService.send(current(authentication), request.phone(), request.amount(), request.note());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wallet/deposits")
    public ResponseEntity<Void> deposit(Authentication authentication, @Valid @RequestBody AmountRequest request) {
        walletService.add(current(authentication), request.amount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/wallet/withdrawals")
    public ResponseEntity<Void> withdraw(Authentication authentication, @Valid @RequestBody AmountRequest request) {
        walletService.withdraw(current(authentication), request.amount());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/money-requests")
    public MoneyRequestsResponse moneyRequests(Authentication authentication) {
        AppUser user = current(authentication);
        return new MoneyRequestsResponse(
                walletService.sentRequests(user).stream().map(MoneyRequestResponse::from).toList(),
                walletService.incomingRequests(user).stream().map(MoneyRequestResponse::from).toList());
    }

    @PostMapping("/money-requests")
    public ResponseEntity<Void> requestMoney(Authentication authentication, @Valid @RequestBody MoneyRequestCreate request) {
        walletService.requestMoney(current(authentication), request.phone(), request.amount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/money-requests/{id}/accept")
    public ResponseEntity<Void> acceptMoneyRequest(Authentication authentication, @PathVariable Long id) {
        walletService.respond(current(authentication), id, true);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/money-requests/{id}/decline")
    public ResponseEntity<Void> declineMoneyRequest(Authentication authentication, @PathVariable Long id) {
        walletService.respond(current(authentication), id, false);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mobile-topups")
    public ResponseEntity<Void> topup(Authentication authentication, @Valid @RequestBody MobileTopupRequest request) {
        walletService.topup(current(authentication), request.operator(), request.number(), request.amount());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bill-payments")
    public ResponseEntity<Void> billPayment(Authentication authentication, @Valid @RequestBody BillPaymentRequest request) {
        walletService.payBill(current(authentication), request.category(), request.billNumber(), request.amount());
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/users/me")
    public UserResponse updateProfile(Authentication authentication, @Valid @RequestBody ProfileUpdateRequest request) {
        AppUser user = current(authentication);
        walletService.updateProfile(user, request.fullName(), emptyToNull(request.email()),
                request.currentPassword(), request.newPassword());
        return UserResponse.from(walletService.current(authentication.getName()));
    }

    private AppUser current(Authentication authentication) {
        return walletService.current(authentication.getName());
    }

    private static String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}