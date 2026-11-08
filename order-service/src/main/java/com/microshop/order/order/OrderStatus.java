package com.microshop.order.order;

import java.util.Set;

/**
 * <pre>
 *  POST /api/orders           POST /api/orders/{id}/payment        Kafka: PaymentSucceeded
 *  ----------------> PENDING_PAYMENT ----------> PAYMENT_PROCESSING ----------> PAID
 *                                                       |   ^
 *                               Kafka: PaymentFailed    v   |  thanh toan lai (sau khi nap tien)
 *                                                  PAYMENT_FAILED
 * </pre>
 */
public enum OrderStatus {
    PENDING_PAYMENT,
    PAYMENT_PROCESSING,
    PAID,
    PAYMENT_FAILED;

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING_PAYMENT, PAYMENT_FAILED -> next == PAYMENT_PROCESSING;
            case PAYMENT_PROCESSING -> Set.of(PAID, PAYMENT_FAILED).contains(next);
            case PAID -> false;
        };
    }
}
