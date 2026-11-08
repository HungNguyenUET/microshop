package com.microshop.payment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * <pre>
 * GET  /api/wallet                 so du (tu tao vi neu chua co)
 * GET  /api/wallet/transactions    lich su giao dich
 * POST /api/wallet/top-up          nap tien demo {amount}
 * </pre>
 * Thanh toan KHONG co REST endpoint: chi xay ra qua Kafka (PaymentRequested).
 */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    public record WalletResponse(String userId, BigDecimal balance, Instant updatedAt) {
        static WalletResponse from(Wallet w) {
            return new WalletResponse(w.getUserId(), w.getBalance(), w.getUpdatedAt());
        }
    }

    public record TransactionResponse(UUID id, String type, BigDecimal amount, String status, UUID orderId,
                                      String reason, Instant createdAt) {
        static TransactionResponse from(WalletTransaction t) {
            return new TransactionResponse(t.getId(), t.getType().name(), t.getAmount(), t.getStatus().name(),
                    t.getOrderId(), t.getReason(), t.getCreatedAt());
        }
    }

    public record TopUpRequest(@NotNull @DecimalMin("10000") BigDecimal amount) {
    }

    private final WalletService wallets;
    private final WalletTransactionRepository transactions;
    private final BigDecimal maxTopUp;

    public WalletController(WalletService wallets, WalletTransactionRepository transactions,
                            @Value("${microshop.wallet.max-top-up}") BigDecimal maxTopUp) {
        this.wallets = wallets;
        this.transactions = transactions;
        this.maxTopUp = maxTopUp;
    }

    @GetMapping
    public WalletResponse get(@AuthenticationPrincipal Jwt jwt) {
        return WalletResponse.from(wallets.getOrCreate(jwt.getSubject()));
    }

    @GetMapping("/transactions")
    public List<TransactionResponse> history(@AuthenticationPrincipal Jwt jwt) {
        return transactions.findTop50ByUserIdOrderByCreatedAtDesc(jwt.getSubject()).stream()
                .map(TransactionResponse::from).toList();
    }

    @PostMapping("/top-up")
    public WalletResponse topUp(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TopUpRequest request) {
        if (request.amount().compareTo(maxTopUp) > 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_CONTENT, "Moi lan nap toi da " + maxTopUp);
        }
        return WalletResponse.from(wallets.topUp(jwt.getSubject(), request.amount()));
    }
}
