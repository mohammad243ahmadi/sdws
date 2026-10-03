package com.sdws.wallet.api;

import com.sdws.wallet.model.AppUser;
import com.sdws.wallet.model.MoneyRequest;
import com.sdws.wallet.model.Wallet;
import com.sdws.wallet.model.WalletTransaction;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {}

    public record LoginRequest(
            @NotBlank String phone,
            @NotBlank String password) {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String fullName,
            @NotBlank @Pattern(regexp = "\\d{9,15}") String phone,
            @Email @Size(max = 254) String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record UserResponse(Long id, String fullName, String phone, String email, String role, boolean active) {
        public static UserResponse from(AppUser user) {
            return new UserResponse(user.getId(), user.getFullName(), user.getPhone(), user.getEmail(),
                    user.getRole().name(), user.isActive());
        }
    }

    public record AuthResponse(UserResponse user) {}

    public record CsrfResponse(String headerName, String token) {}

    public record WalletResponse(Long userId, String ownerName, String phone, String accountNumber, BigDecimal balance) {
        public static WalletResponse from(Wallet wallet) {
            AppUser user = wallet.getUser();
            return new WalletResponse(user.getId(), user.getFullName(), user.getPhone(), wallet.getAccountNumber(), wallet.getBalance());
        }
    }

    public record TransactionResponse(Long id, String type, BigDecimal amount, String status, String description,
                                       LocalDateTime createdAt, String userName, String userPhone) {
        public static TransactionResponse from(WalletTransaction transaction) {
            AppUser user = transaction.getUser();
            return new TransactionResponse(transaction.getId(), transaction.getType().name(), transaction.getAmount(),
                    transaction.getStatus().name(), transaction.getDescription(), transaction.getCreatedAt(),
                    user.getFullName(), user.getPhone());
        }
    }

    public record UserSummary(Long id, String fullName, String phone) {
        public static UserSummary from(AppUser user) {
            return new UserSummary(user.getId(), user.getFullName(), user.getPhone());
        }
    }

    public record MoneyRequestResponse(Long id, UserSummary requester, UserSummary target, BigDecimal amount,
                                       String status, LocalDateTime createdAt) {
        public static MoneyRequestResponse from(MoneyRequest request) {
            return new MoneyRequestResponse(request.getId(), UserSummary.from(request.getRequester()),
                    UserSummary.from(request.getTarget()), request.getAmount(), request.getStatus().name(), request.getCreatedAt());
        }
    }

    public record MoneyRequestsResponse(List<MoneyRequestResponse> sent, List<MoneyRequestResponse> incoming) {}

    public record AmountRequest(
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}

    public record TransferRequest(
            @NotBlank @Pattern(regexp = "\\d{9,15}") String phone,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount,
            @Size(max = 250) String note) {}

    public record MoneyRequestCreate(
            @NotBlank @Pattern(regexp = "\\d{9,15}") String phone,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}

    public record MobileTopupRequest(
            @NotBlank @Pattern(regexp = "Roshan|AWCC|Etisalat|MTN|Salaam") String operator,
            @NotBlank @Pattern(regexp = "\\d{9,15}") String number,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}

    public record BillPaymentRequest(
            @NotBlank @Pattern(regexp = "Electricity|Water|Internet|TV|Other") String category,
            @NotBlank @Size(max = 100) String billNumber,
            @NotNull @DecimalMin("0.01") @Digits(integer = 17, fraction = 2) BigDecimal amount) {}

    public record ProfileUpdateRequest(
            @NotBlank @Size(max = 100) String fullName,
            @Email @Size(max = 254) String email,
            String currentPassword,
            @Size(min = 8, max = 100) String newPassword) {}

        public record ActiveStatusRequest(@NotNull Boolean active) {}

        public record AdminOverviewResponse(long userCount, long activeCount, long transactionCount,
                                                                                BigDecimal totalBalance, List<TransactionResponse> latestTransactions) {}
}