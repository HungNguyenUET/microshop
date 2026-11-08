package com.microshop.payment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, UUID> {

    Optional<WalletTransaction> findByEventId(UUID eventId);

    boolean existsByOrderIdAndTypeAndStatus(UUID orderId, WalletTransaction.Type type, WalletTransaction.Status status);

    List<WalletTransaction> findTop50ByUserIdOrderByCreatedAtDesc(String userId);
}
