package com.microshop.order.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "failure_reason")
    private String failureReason;

    /** Optimistic lock: 2 request/event cung sua 1 order -> 1 cai bi loi thay vi ghi de am tham. */
    @Version
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {
    }

    public Order(String userId, String username) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.username = username;
        this.status = OrderStatus.PENDING_PAYMENT;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void addItem(UUID productId, String productName, BigDecimal unitPrice, int quantity) {
        items.add(new OrderItem(this, productId, productName, unitPrice, quantity));
        totalAmount = totalAmount.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    /** @return false neu khong chuyen duoc (event den tre/trung) -> bo qua, khong nem loi. */
    public boolean moveTo(OrderStatus next, String reason) {
        if (!status.canMoveTo(next)) {
            return false;
        }
        this.status = next;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
        return true;
    }

    public UUID getId() { return id; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
}
