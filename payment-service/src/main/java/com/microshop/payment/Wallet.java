package com.microshop.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "wallet")
public class Wallet {

    @Id
    @Column(name = "user_id")
    private String userId;

    @Column(nullable = false)
    private BigDecimal balance;

    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Wallet() {
    }

    public Wallet(String userId, BigDecimal initialBalance) {
        this.userId = userId;
        this.balance = initialBalance;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public boolean canPay(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public void debit(BigDecimal amount) {
        if (!canPay(amount)) {
            throw new IllegalStateException("Insufficient balance");
        }
        balance = balance.subtract(amount);
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        balance = balance.add(amount);
        updatedAt = Instant.now();
    }

    public String getUserId() { return userId; }
    public BigDecimal getBalance() { return balance; }
    public Instant getUpdatedAt() { return updatedAt; }
}
