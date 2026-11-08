package com.microshop.order.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findByUserIdOrderByAddedAt(String userId);

    Optional<CartItem> findByUserIdAndProductId(String userId, UUID productId);

    void deleteByUserIdAndProductId(String userId, UUID productId);

    void deleteByUserId(String userId);
}
