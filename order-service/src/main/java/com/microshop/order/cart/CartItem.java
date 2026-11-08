package com.microshop.order.cart;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cart_item")
public class CartItem {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "added_at", nullable = false)
    private Instant addedAt;

    protected CartItem() {
    }

    public CartItem(String userId, UUID productId, int quantity) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.productId = productId;
        this.quantity = quantity;
        this.addedAt = Instant.now();
    }

    public void increase(int amount) {
        this.quantity += amount;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public UUID getProductId() { return productId; }
    public int getQuantity() { return quantity; }
}
