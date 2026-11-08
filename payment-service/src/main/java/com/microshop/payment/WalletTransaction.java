package com.microshop.payment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction {

    public enum Type { TOP_UP, PAYMENT }

    public enum Status { SUCCESS, FAILED }

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "order_id")
    private UUID orderId;

    private String reason;

    @Column(name = "event_id", unique = true)
    private UUID eventId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WalletTransaction() {
    }

    private WalletTransaction(String userId, Type type, BigDecimal amount, Status status, UUID orderId,
                              String reason, UUID eventId) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.type = type;
        this.amount = amount;
        this.status = status;
        this.orderId = orderId;
        this.reason = reason;
        this.eventId = eventId;
        this.createdAt = Instant.now();
    }

    public static WalletTransaction topUp(String userId, BigDecimal amount) {
        return new WalletTransaction(userId, Type.TOP_UP, amount, Status.SUCCESS, null, null, null);
    }

    public static WalletTransaction payment(String userId, BigDecimal amount, UUID orderId, UUID eventId) {
        return new WalletTransaction(userId, Type.PAYMENT, amount, Status.SUCCESS, orderId, null, eventId);
    }

    public static WalletTransaction failedPayment(String userId, BigDecimal amount, UUID orderId, UUID eventId, String reason) {
        return new WalletTransaction(userId, Type.PAYMENT, amount, Status.FAILED, orderId, reason, eventId);
    }

    public UUID getId() { return id; }
    public Type getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Status getStatus() { return status; }
    public UUID getOrderId() { return orderId; }
    public String getReason() { return reason; }
    public Instant getCreatedAt() { return createdAt; }
}
