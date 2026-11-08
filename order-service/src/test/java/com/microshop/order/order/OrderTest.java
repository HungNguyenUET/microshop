package com.microshop.order.order;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    private Order newOrder() {
        Order order = new Order("u-1", "user");
        order.addItem(UUID.randomUUID(), "Ban phim", new BigDecimal("1290000"), 2);
        order.addItem(UUID.randomUUID(), "Chuot", new BigDecimal("450000"), 1);
        return order;
    }

    @Test
    void totalIsSumOfLines() {
        assertThat(newOrder().getTotalAmount()).isEqualByComparingTo("3030000");
    }

    @Test
    void paymentHappyPath() {
        Order order = newOrder();
        assertThat(order.moveTo(OrderStatus.PAYMENT_PROCESSING, null)).isTrue();
        assertThat(order.moveTo(OrderStatus.PAID, null)).isTrue();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void canRetryAfterFailedPayment() {
        Order order = newOrder();
        order.moveTo(OrderStatus.PAYMENT_PROCESSING, null);
        order.moveTo(OrderStatus.PAYMENT_FAILED, "So du khong du");
        assertThat(order.getFailureReason()).isEqualTo("So du khong du");
        assertThat(order.moveTo(OrderStatus.PAYMENT_PROCESSING, null)).isTrue();
        assertThat(order.getFailureReason()).isNull();
    }

    @Test
    void paidOrderCannotBePaidAgain() {
        Order order = newOrder();
        order.moveTo(OrderStatus.PAYMENT_PROCESSING, null);
        order.moveTo(OrderStatus.PAID, null);
        assertThat(order.moveTo(OrderStatus.PAYMENT_PROCESSING, null)).isFalse();
        assertThat(order.moveTo(OrderStatus.PAYMENT_FAILED, "late event")).isFalse();
    }
}
