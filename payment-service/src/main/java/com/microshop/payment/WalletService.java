package com.microshop.payment;

import com.microshop.events.OrderEvent.PaymentRequested;
import com.microshop.events.PaymentEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);

    private final WalletRepository wallets;
    private final WalletTransactionRepository transactions;
    private final BigDecimal initialBalance;
    private final MeterRegistry metrics;

    public WalletService(WalletRepository wallets, WalletTransactionRepository transactions,
                         @Value("${microshop.wallet.initial-balance}") BigDecimal initialBalance, MeterRegistry metrics) {
        this.wallets = wallets;
        this.transactions = transactions;
        this.initialBalance = initialBalance;
        this.metrics = metrics;
    }

    /** Custom metric: microshop_payments_total{result="success|failed"}. */
    private void count(String result) {
        metrics.counter("microshop.payments", "result", result).increment();
    }

    /** User moi chua co vi -> tu tao vi voi so du demo. */
    @Transactional
    public Wallet getOrCreate(String userId) {
        return wallets.findById(userId).orElseGet(() -> {
            try {
                return wallets.saveAndFlush(new Wallet(userId, initialBalance));
            } catch (DataIntegrityViolationException race) {
                return wallets.findById(userId).orElseThrow();
            }
        });
    }

    @Transactional
    public Wallet topUp(String userId, BigDecimal amount) {
        getOrCreate(userId);
        Wallet wallet = wallets.findForUpdate(userId).orElseThrow();
        wallet.credit(amount);
        transactions.save(WalletTransaction.topUp(userId, amount));
        return wallet;
    }

    /**
     * Xu ly yeu cau thanh toan tu Kafka, tra ve event ket qua de gui lai cho order-service.
     */
    @Transactional
    public Optional<PaymentEvent> pay(PaymentRequested request) {
        // 1. Idempotency: Kafka la "at-least-once", cung 1 event co the den 2 lan.
        //    Da xu ly roi -> KHONG tru tien lan nua, chi gui lai ket qua cu
        //    (phong truong hop lan truoc commit DB xong nhung chua kip gui ket qua thi app chet).
        var previous = transactions.findByEventId(request.eventId());
        if (previous.isPresent()) {
            log.warn("Duplicate PaymentRequested event {}, re-send previous result", request.eventId());
            return Optional.of(toResultEvent(previous.get(), request));
        }
        // 2. Don da tra tien roi (vd: user bam thanh toan 2 lan) -> khong tru nua
        if (transactions.existsByOrderIdAndTypeAndStatus(request.orderId(), WalletTransaction.Type.PAYMENT,
                WalletTransaction.Status.SUCCESS)) {
            log.warn("Order {} already paid, skip", request.orderId());
            return Optional.empty();
        }

        getOrCreate(request.userId());
        Wallet wallet = wallets.findForUpdate(request.userId()).orElseThrow();   // khoa dong vi

        if (!wallet.canPay(request.amount())) {
            String reason = "Số dư không đủ (cần " + request.amount() + ", hiện có " + wallet.getBalance() + ")";
            transactions.save(WalletTransaction.failedPayment(request.userId(), request.amount(), request.orderId(),
                    request.eventId(), reason));
            log.info("Payment FAILED for order {}: {}", request.orderId(), reason);
            count("failed");
            return Optional.of(new PaymentEvent.PaymentFailed(UUID.randomUUID(), request.orderId(), request.userId(),
                    reason, Instant.now()));
        }

        wallet.debit(request.amount());   // UPDATE wallet SET balance = balance - ? (cung transaction voi INSERT ben duoi)
        transactions.save(WalletTransaction.payment(request.userId(), request.amount(), request.orderId(), request.eventId()));
        count("success");
        log.info("Payment SUCCESS for order {}: -{} -> balance {}", request.orderId(), request.amount(), wallet.getBalance());
        return Optional.of(new PaymentEvent.PaymentSucceeded(UUID.randomUUID(), request.orderId(), request.userId(),
                request.amount(), wallet.getBalance(), Instant.now()));
    }

    private PaymentEvent toResultEvent(WalletTransaction tx, PaymentRequested request) {
        return tx.getStatus() == WalletTransaction.Status.SUCCESS
                ? new PaymentEvent.PaymentSucceeded(UUID.randomUUID(), request.orderId(), request.userId(),
                tx.getAmount(), getOrCreate(request.userId()).getBalance(), Instant.now())
                : new PaymentEvent.PaymentFailed(UUID.randomUUID(), request.orderId(), request.userId(),
                tx.getReason(), Instant.now());
    }
}
